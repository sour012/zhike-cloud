# 智课云 · 教学管理系统

面向高校教务场景的全栈系统：**课程库 → 排课开课 → 学生选课 → 教师录成绩 → 成绩单与绩点**，配一套 RBAC 权限（菜单 + 按钮级）与操作日志。
后端 Spring Boot 3 + MyBatis-Plus + MySQL 8，前端 Vue 3 + TypeScript + Element Plus，前后端分离、JWT 无状态鉴权。

**这个项目想说明的不是「能增删改查」，而是三个真实存在的麻烦点怎么处理：**

| 麻烦点 | 处理方式 |
| --- | --- |
| 选课瞬间抢名额，容易超卖 | 条件 `UPDATE ... SET enrolled = enrolled + 1 WHERE enrolled < capacity` 原子占座，不先查后改 |
| 一个学生的课表不能撞时间 | 同一天节次区间求交，冲突在服务端拒绝，前端只做置灰提示 |
| 权限容易只做在菜单上 | 菜单、按钮、接口三处读同一份权限点，接口用 `@PreAuthorize` 真正拦截 |

---

## 1. 功能矩阵

| 模块 | 管理员 | 教师 | 学生 |
| --- | --- | --- | --- |
| 工作台 | 全局统计 | 全局统计 | 我的选课 / 学分 / 绩点 |
| 课程库 | 增删改查 | 查看 | 查看 |
| 开课管理 | 增删改查 | 增改自己的班 | 查看 + 选课 |
| 我的课表 | — | 查看 | 选课 / 退课 |
| 成绩管理 | 全部成绩 | 只录自己班 | 只看自己 |
| 用户 / 角色 | 增删改 + 授权 | — | — |
| 通知公告 | 发布 / 编辑 / 删除 | 查看 | 查看 |

权限点示例：`edu:course:add`、`edu:enroll:select`、`edu:score:add`、`system:user:delete`。

## 2. 技术栈

| 层 | 选型 | 说明 |
| --- | --- | --- |
| 后端 | Spring Boot 3.2 / Java 21 | Web、Validation、AOP |
| 持久层 | MyBatis-Plus 3.5 | 单表用 Wrapper，多表 JOIN 写注解 SQL，分页插件统一 |
| 数据库 | MySQL 8（生产）/ H2 内存库（测试） | 同一份 DDL 两边都能跑，测试不用起 MySQL |
| 鉴权 | Spring Security 6 + JWT（HS256） | 无状态，令牌里带角色与权限点 |
| 接口文档 | springdoc-openapi | 启动后 `/api/doc.html` |
| 前端 | Vue 3.5 + TS + Vite 5 | Composition API + `<script setup>` |
| UI | Element Plus 2.8 | 表格、表单、树、进度条 |
| 状态 | Pinia | 登录态、权限集合 |

## 3. 架构

```
┌───────────────────────────────────────────────┐
│  Vue 3 SPA（Element Plus）                     │
│  登录 → Pinia 存令牌与 perms → 路由守卫按 perm 拦 │
│  侧边栏菜单由 /auth/menus 下发（不是前端写死）    │
└───────────────────────┬───────────────────────┘
                        │ HTTP /api/*  Authorization: Bearer <jwt>
┌───────────────────────▼───────────────────────┐
│  Spring Security 过滤链                        │
│  JwtAuthenticationFilter → SecurityContext     │
│  未认证 401 / 无权限 403（JSON，不走默认页面）    │
└───────────────────────┬───────────────────────┘
                        │
     ┌──────────────────┼──────────────────┬──────────────┐
     ▼                  ▼                  ▼              ▼
  Controller        Service            Mapper         Aspect
  @PreAuthorize     业务规则/事务       MyBatis-Plus   @LogOperation
  参数校验          冲突/容量/归属      JOIN 查询      写 sys_oper_log
```

## 4. 关键设计

### 4.1 选课并发：条件更新代替「先查后改」

```java
// OfferingMapper
@Update("UPDATE edu_offering SET enrolled = enrolled + 1 "
      + "WHERE id = #{id} AND enrolled < capacity AND status = 1")
int tryOccupy(@Param("id") Long id);
```

返回 0 即代表「名额已满或课程已关闭」，直接抛业务异常。判断和写入在同一条 SQL 里完成，
不会出现「查到还有 1 个名额 → 另一个事务抢走 → 自己再写」的窗口。
`edu_enrollment` 上还有 `(offering_id, student_id)` 唯一索引兜底重复选课。
退课用 `enrolled > 0` 的条件更新，保证计数不会被减成负数。

### 4.2 时间冲突：区间求交

同一学期内，把该学生所有「未退课」的时段拉出来，与本课程比较：
`weekday` 相同且 `start1 <= end2 && start2 <= end1` 即冲突。
冲突课程名会带回前端，直接告诉他撞了哪门课，而不是一句「选课失败」。

### 4.3 权限：三处读同一份数据

- `sys_menu` 里既有菜单行也有按钮行，按钮行的 `perm` 就是权限点；
- 角色通过 `sys_role_menu` 授权，登录时把权限点集合写进 JWT；
- 前端路由守卫用 `perms` 拦页面，按钮用 `v-if="store.has('edu:course:add')"`；
- 后端在方法上写 `@PreAuthorize("hasAuthority('edu:course:add')")`。

只做前端隐藏等于没做，所以接口这层是硬拦截（`AuthAndPermissionTest` 覆盖了学生、教师访问 `/users` 均返回 403）。

### 4.4 成绩：不让前端参与计算

总评 = 平时 × 30% + 考试 × 70%，再按学校 4.0 制换算绩点，全部在 `ScoreService` 完成。
加权绩点 = Σ(绩点 × 学分) / Σ学分，SQL 里聚合，避免把整张成绩单拉到内存里算。
教师只能给自己任课的班录成绩，学生只能看到自己的行——范围由服务端按令牌里的身份收窄，不信任前端传参。

### 4.5 操作日志

`@LogOperation(module, action)` 标注在写接口上，AOP 记录操作人、参数（截断 900 字）、IP、耗时、成功与否。
日志写入失败只 warn，不影响主流程——审计是旁路，不能因为它把选课搞挂。

## 5. 快速开始

### 5.0 克隆代码

```bash
git clone git@github.com:sour012/zhike-cloud.git
cd zhike-cloud
```

### 5.1 准备数据库（MySQL 8）

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS zhike_cloud DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"

mysql -uroot -p zhike_cloud < backend/src/main/resources/db/schema.sql
mysql -uroot -p zhike_cloud < backend/src/main/resources/db/data.sql
mysql -uroot -p zhike_cloud < sql/indexes.sql
```

导入后应有：13 张表、13 个教学班、23 条选课记录、7 条成绩、3 条公告。

### 5.1b 不想装 MySQL？用演示模式（H2 内存库）

```bash
cd backend && mvn clean package -DskipTests
java -jar target/zhike-cloud-0.1.0.jar --spring.profiles.active=demo
```

启动时自动建表灌数据，进程结束数据即丢弃，适合面试现场演示，不需要任何外部依赖。

### 5.2 启动后端

```bash
cd backend
# 数据库密码通过环境变量传，不要写进配置文件
export DB_PASSWORD='你的密码'        # Windows PowerShell: $env:DB_PASSWORD="你的密码"
mvn spring-boot:run
```

启动后：
- 接口地址 `http://127.0.0.1:8088/api`
- 接口文档 `http://127.0.0.1:8088/api/doc.html`

### 5.3 启动前端

```bash
cd frontend
npm install
npm run dev        # http://127.0.0.1:5173
```

Vite 已把 `/api` 代理到 `127.0.0.1:8088`，开发时不用处理跨域。

### 5.4 打包部署

```bash
cd frontend && npm run build          # 产出 dist/
cp -r dist/* ../backend/src/main/resources/static/
cd ../backend && mvn clean package -DskipTests
java -jar target/zhike-cloud-0.1.0.jar
```

前端用 Hash 路由，静态资源直接放 `static/` 即可，刷新不会 404。

## 6. 演示账号

| 角色 | 用户名 | 密码 | 能看到什么 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 全部功能，含用户与角色授权 |
| 教师 | `teacher1` | `teacher123` | 王爱国：数据结构 / 数据库原理 / 数据挖掘，可录入成绩 |
| 学生 | `student1` | `student123` | 张平：5 门已选、上学期 5 门成绩、加权绩点 3.53 |

## 7. 目录结构

```
zhike-cloud/
├── backend/
│   ├── src/main/java/com/zhike/
│   │   ├── common/      统一返回、分页、异常处理
│   │   ├── config/      Security、MyBatis-Plus
│   │   ├── security/    JWT、登录态主体、当前用户
│   │   ├── system/      用户 / 角色 / 菜单 / 认证
│   │   ├── edu/         课程 / 开课 / 选课 / 成绩 / 看板
│   │   ├── notice/      公告
│   │   └── log/         操作日志切面
│   ├── src/main/resources/db/   schema.sql、data.sql
│   └── src/test/        H2 集成测试
├── frontend/            Vue 3 + TS + Element Plus
├── sql/indexes.sql      生产索引
└── docs/                DESIGN.md、INTERVIEW.md
```

## 8. 测试

测试跑在 H2（MySQL 兼容模式）上，不需要本地 MySQL：

```bash
cd backend && mvn test
```

覆盖的场景（共 22 个用例，全部通过）：

| 测试类 | 覆盖内容 |
| --- | --- |
| `AuthAndPermissionTest` | 登录成功/失败、伪造令牌、学生与教师访问管理接口被拒、菜单按角色下发、请求体格式错 400、动词错 405、教师改角色 403 |
| `EnrollmentTest` | 时间冲突、重复选课、最后一个名额不超卖、退课释放名额、已录成绩不可退、只能退自己的课、我的课表默认当前学期 |
| `ScoreTest` | 总评与绩点计算、教师跨班录入被拒、学生只看自己、加权绩点、分数越界校验 |
| `AdminCrudTest` | 课程编号重复 409、有开课记录不可删、公告权限、操作日志落库 |

### 8.1 真实 MySQL 也跑过一遍

H2 只能保证语法层兼容，索引行为、锁粒度这些东西必须真库验。已在 MySQL 8.0.46（utf8mb4 / InnoDB）上完成：

- 三份脚本执行成功，13 张表 + 17 个二级索引就位，中文无乱码；
- 三角色登录权限点数：admin 23 / teacher 10 / student 8；
- **并发抢课**：新建容量 = 1 的教学班，3 个学生用线程栅栏同时提交，
  结果恰好 1 个 200、2 个 409「该课程名额已满」，库内 `enrolled = 1/1`，没有超卖；
- 时间冲突 409、重复选课 409、退课释放名额、教师跨班查名单 403、学生查用户列表 403、无令牌 401；
- 教师录入 85/92 → 总评 89.9、绩点 3.7；学生加权绩点 3.53；
- `sys_oper_log` 按操作落库（AOP 生效）。

## 9. 已知局限

写在前面，免得被当成「没考虑到」：

1. **单库单表，没有分库分表**。选课高峰的瓶颈会先出现在 `edu_offering` 的行锁上，
   真实场景应把热点课程的名额放到 Redis（`DECR` 预扣）+ 异步落库，本项目用数据库行锁是刻意选的「够用且不引入中间件」。
2. **权限变更需重新登录**。令牌里带 perms，TTL 8 小时；改角色后旧令牌仍持有旧权限。
   要即时生效得把 perms 放 Redis 并在授权时主动失效，本机没有 Redis，这点如实说明。
3. **没有排课算法**。开课时间由管理员手工录入，系统只做冲突校验，不做自动排课。
4. **审计日志不归档**。`sys_oper_log` 会持续增长，生产需要按天分区或定期清理。
5. **前端未做单元测试**，交互逻辑靠手工点；后端接口有集成测试兜底。
