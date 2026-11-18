<template>
  <div class="login-page">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">智</span>
        <span class="brand-name">智课云教学管理系统</span>
      </div>
      <span class="topbar-note">教务处 · 统一身份认证平台</span>
    </header>

    <main class="main">
      <section class="panel">
        <div class="panel-head">
          <span class="panel-mark">智</span>
          <h1>统一身份认证</h1>
          <p class="panel-sub">请使用教务系统账号登录</p>
        </div>

        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="学号 / 工号 / 管理员账号"
              size="large"
              autocomplete="username"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入登录密码"
              size="large"
              show-password
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>

          <div class="form-row">
            <el-checkbox v-model="remember" size="small">记住用户名</el-checkbox>
            <el-link type="primary" :underline="false" @click="forget">忘记密码？</el-link>
          </div>

          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <div class="contact">
          账号异常请联系教务处 <strong>028-8709 2100</strong>
        </div>

        <div class="demo-toggle">
          <el-link :underline="false" @click="showDemo = !showDemo">
            {{ showDemo ? '收起演示账号' : '查看演示账号' }}
          </el-link>
        </div>

        <el-collapse-transition>
          <div v-show="showDemo" class="demo">
            <div
              v-for="d in demos"
              :key="d.username"
              class="demo-item"
              @click="fill(d)"
            >
              <span class="demo-role">{{ d.label }}</span>
              <span class="demo-account">{{ d.username }} / {{ d.password }}</span>
              <span class="demo-fill">填入</span>
            </div>
          </div>
        </el-collapse-transition>
      </section>
    </main>

    <footer class="footer">
      <span>© 2026 智课云教学管理系统 · 教务处</span>
      <span class="footer-sep">|</span>
      <span>建议使用 Chrome / Edge，1366×768 以上分辨率</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showDemo = ref(false)
const remember = ref(true)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const demos = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '教师', username: 'teacher1', password: 'teacher123' },
  { label: '学生', username: 'student1', password: 'student123' },
]

onMounted(() => {
  const saved = localStorage.getItem('zk:last-username')
  if (saved) {
    form.username = saved
    form.password = ''
  }
})

function fill(d: { username: string; password: string }) {
  form.username = d.username
  form.password = d.password
}

function forget() {
  ElMessage.info('请联系教务处重置密码（028-8709 2100）')
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      await store.login(form.username, form.password)
      if (remember.value) {
        localStorage.setItem('zk:last-username', form.username)
      } else {
        localStorage.removeItem('zk:last-username')
      }
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
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f2f5fa;
}

/* 顶部信息栏：系统名靠左，认证平台靠右 */
.topbar {
  height: 60px;
  padding: 0 32px;
  background: #fff;
  border-bottom: 1px solid #e5eaf3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: var(--zk-primary);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 15px;
  font-weight: 600;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.topbar-note {
  font-size: 13px;
  color: var(--zk-muted);
}

/* 主体：卡片居中 */
.main {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 40px 16px;
}

.panel {
  width: 400px;
  max-width: 100%;
  background: #fff;
  border: 1px solid #e5eaf3;
  border-radius: 6px;
  box-shadow: 0 6px 24px rgba(22, 34, 74, 0.06);
  padding: 34px 36px 26px;
}

.panel-head {
  text-align: center;
  margin-bottom: 24px;
}

.panel-mark {
  display: inline-grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #eef2fd;
  color: var(--zk-primary);
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
}

.panel-head h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.panel-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--zk-muted);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 42px;
  letter-spacing: 4px;
  border-radius: 4px;
}

.contact {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #e8edf6;
  font-size: 12px;
  line-height: 1.7;
  color: var(--zk-muted);
  text-align: center;
}

.contact strong {
  color: #4a5468;
  font-weight: 600;
}

.demo-toggle {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
}

.demo {
  margin-top: 12px;
  border: 1px solid #e8edf6;
  border-radius: 4px;
  background: #fafbfe;
  overflow: hidden;
}

.demo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  font-size: 12.5px;
  cursor: pointer;
  border-bottom: 1px solid #eef2f8;
}

.demo-item:last-child {
  border-bottom: none;
}

.demo-item:hover {
  background: #eef2fd;
}

.demo-role {
  color: #4a5468;
  width: 42px;
  flex: none;
}

.demo-account {
  color: var(--zk-muted);
  font-family: ui-monospace, Consolas, monospace;
  flex: 1;
}

.demo-fill {
  color: var(--zk-primary);
  flex: none;
}

.footer {
  flex: none;
  padding: 16px 24px 20px;
  text-align: center;
  font-size: 12px;
  color: #9aa4b8;
}

.footer-sep {
  margin: 0 10px;
  color: #ccd4e2;
}

@media (max-width: 640px) {
  .topbar {
    padding: 0 16px;
  }

  .topbar-note {
    display: none;
  }

  .panel {
    padding: 26px 22px 20px;
  }
}
</style>
