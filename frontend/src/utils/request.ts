import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'zhike_token'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 后端统一 {code,msg,data}，code=0 才算成功
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body?.data
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg || error.message || '网络异常'
    if (status === 401) {
      // 令牌失效：清掉本地状态并回登录页，不做静默重试
      localStorage.removeItem(TOKEN_KEY)
      if (!location.hash.includes('/login')) {
        location.hash = '#/login'
      }
      ElMessage.error(msg || '登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error(msg || '没有权限执行该操作')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  },
)

export default request
