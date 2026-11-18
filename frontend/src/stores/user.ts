import { defineStore } from 'pinia'
import { authApi, type MenuNode, type UserProfile } from '@/api'
import { TOKEN_KEY } from '@/utils/request'

interface State {
  token: string
  profile: UserProfile | null
  menus: MenuNode[]
}

export const useUserStore = defineStore('user', {
  state: (): State => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: null,
    menus: [],
  }),

  getters: {
    permSet: (s) => new Set(s.profile?.perms ?? []),
    isAdmin: (s) => (s.profile?.roles ?? []).includes('admin'),
    isTeacher: (s) => s.profile?.userType === 2,
    isStudent: (s) => s.profile?.userType === 3,
    displayName: (s) => s.profile?.realName || s.profile?.username || '',
    roleLabel: (s) => {
      const t = s.profile?.userType
      return t === 1 ? '管理员' : t === 2 ? '教师' : '学生'
    },
  },

  actions: {
    async login(username: string, password: string) {
      const res = await authApi.login({ username, password })
      this.token = res.token
      this.profile = res.user
      localStorage.setItem(TOKEN_KEY, res.token)
      await this.loadMenus()
    },

    async loadProfile() {
      if (!this.token) return
      this.profile = await authApi.profile()
    },

    async loadMenus() {
      if (!this.token) return
      this.menus = await authApi.menus()
    },

    has(perm?: string) {
      if (!perm) return true
      return this.permSet.has(perm)
    },

    logout() {
      this.token = ''
      this.profile = null
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
      authApi.logout().catch(() => undefined)
    },
  },
})
