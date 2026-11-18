import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', perm: 'dashboard:view' },
      },
      {
        path: 'edu/courses',
        name: 'courses',
        component: () => import('@/views/Courses.vue'),
        meta: { title: '课程库', perm: 'edu:course:list' },
      },
      {
        path: 'edu/offerings',
        name: 'offerings',
        component: () => import('@/views/Offerings.vue'),
        meta: { title: '开课管理', perm: 'edu:offering:list' },
      },
      {
        path: 'edu/my-courses',
        name: 'myCourses',
        component: () => import('@/views/MyCourses.vue'),
        meta: { title: '我的课表', perm: 'edu:enroll:list' },
      },
      {
        path: 'edu/scores',
        name: 'scores',
        component: () => import('@/views/Scores.vue'),
        meta: { title: '成绩管理', perm: 'edu:score:list' },
      },
      {
        path: 'system/users',
        name: 'users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', perm: 'system:user:list' },
      },
      {
        path: 'system/roles',
        name: 'roles',
        component: () => import('@/views/Roles.vue'),
        meta: { title: '角色权限', perm: 'system:role:list' },
      },
      {
        path: 'notice',
        name: 'notices',
        component: () => import('@/views/Notices.vue'),
        meta: { title: '通知公告', perm: 'notice:list' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

/**
 * 路由守卫：没有令牌去登录页；有令牌但没载入画像就先补一次（刷新页面场景）；
 * 没有对应权限点时退回工作台，避免出现空白页。
 */
router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.meta.public) {
    return true
  }
  if (!store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!store.profile) {
    try {
      await store.loadProfile()
      await store.loadMenus()
    } catch {
      store.logout()
      return { path: '/login' }
    }
  }
  const perm = to.meta.perm as string | undefined
  if (perm && !store.has(perm)) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
