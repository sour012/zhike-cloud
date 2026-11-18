<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="brand-logo">智</div>
        <div>
          <div class="brand-title">智课云</div>
          <div class="brand-sub">教学管理系统</div>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="menu" background-color="transparent">
        <template v-for="menu in store.menus" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="String(menu.id)">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path || ''">
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path || ''">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="crumb">{{ route.meta.title || '智课云' }}</span>
        </div>
        <div class="header-right">
          <el-tag size="small" type="info" effect="plain">{{ store.roleLabel }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28">{{ store.displayName.slice(0, 1) }}</el-avatar>
              <span>{{ store.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #101a33;
  color: #cfd7e8;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px 14px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #4a6bff, #2f54eb);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
}

.brand-title {
  color: #fff;
  font-weight: 600;
  font-size: 15px;
}

.brand-sub {
  font-size: 11px;
  color: #7d8aa8;
}

.menu {
  border-right: none;
  --el-menu-text-color: #c2cbdd;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #ffffff;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(74, 107, 255, 0.9), rgba(47, 84, 235, 0.65));
  border-radius: 8px;
  margin: 2px 8px;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  border-radius: 8px;
  margin: 2px 8px;
}

.header {
  background: #fff;
  border-bottom: 1px solid #edf0f6;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.crumb {
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
}

.main {
  padding: 0;
  background: var(--zk-bg);
  overflow-y: auto;
}
</style>
