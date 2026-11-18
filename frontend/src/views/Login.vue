<template>
  <div class="login-wrap">
    <div class="login-left">
      <div class="brand">
        <div class="logo">智</div>
        <div>
          <h1>智课云</h1>
          <p>教学管理系统 · 课程 / 选课 / 成绩 / 权限</p>
        </div>
      </div>
      <ul class="points">
        <li>按角色划分菜单与按钮：管理员、教师、学生三套视图</li>
        <li>选课并发控制：条件更新原子占座，名额不超卖</li>
        <li>时间冲突、选课门数、已录成绩退课，四类规则前置校验</li>
        <li>成绩总评与绩点由后端统一计算，避免前端篡改</li>
      </ul>
    </div>

    <div class="login-right">
      <el-card class="login-card" shadow="never">
        <h2>账号登录</h2>
        <p class="sub">使用教务分配的账号登录</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" @keyup.enter="submit" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>

        <el-divider>演示账号</el-divider>
        <div class="demo">
          <span v-for="d in demos" :key="d.username" class="demo-item" @click="fill(d)">
            {{ d.label }}：{{ d.username }} / {{ d.password }}
          </span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const demos = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '教师', username: 'teacher1', password: 'teacher123' },
  { label: '学生', username: 'student1', password: 'student123' },
]

function fill(d: { username: string; password: string }) {
  form.username = d.username
  form.password = d.password
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      await store.login(form.username, form.password)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/dashboard'
      router.push(redirect)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-wrap {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  height: 100vh;
}

.login-left {
  background: linear-gradient(150deg, #16224a 0%, #22336b 55%, #2f54eb 130%);
  color: #fff;
  padding: 56px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 36px;
}

.brand {
  display: flex;
  gap: 14px;
  align-items: center;
}

.logo {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
}

.brand h1 {
  margin: 0 0 6px;
  font-size: 24px;
}

.brand p {
  margin: 0;
  color: #b9c4de;
  font-size: 13px;
}

.points {
  margin: 0;
  padding-left: 18px;
  line-height: 2;
  color: #cdd6ea;
  font-size: 13.5px;
}

.login-right {
  display: grid;
  place-items: center;
  background: #f6f8fc;
}

.login-card {
  width: 380px;
  border-radius: 14px;
  padding: 8px 6px;
}

.login-card h2 {
  margin: 4px 0 6px;
  font-size: 20px;
}

.sub {
  color: var(--zk-muted);
  font-size: 13px;
  margin: 0 0 18px;
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.demo {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.demo-item {
  font-size: 12.5px;
  color: var(--zk-muted);
  cursor: pointer;
}

.demo-item:hover {
  color: var(--zk-primary);
}
</style>
