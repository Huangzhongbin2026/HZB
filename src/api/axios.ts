import axios from 'axios'
import type { ResponseData } from '@one/core'
// 变更全局类型以符合接口规范
declare module 'axios' {
  export interface AxiosResponse<T = any> extends ResponseData<T> {}
}
// 这里可以自定义一些配置，例如baseURL / timeout 等
const axios$ = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})
// 任务服务接口要求 Authorization，这里统一补充本地联调令牌。
axios$.interceptors.request.use((config) => {
  const token = localStorage.getItem('DEV_AUTH_TOKEN') || 'Bearer dev-local-token'
  config.headers = config.headers || {}
  if (!config.headers.Authorization) {
    config.headers.Authorization = token
  }
  return config
})
// 使用拦截器处理全局数据（token逻辑已由内核统一处理，添加请求头请慎重）
axios$.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const data: ResponseData = error.response.data
    if (data?.code) {
      console.error(data.stack, error.request)
      if (data.code === '500') {
        // 如果500则交给前端自定义提示，否则提示的一般是业务逻辑错误
        error.message = null
      } else {
        // 如果非500则提示服务端捕获错误信息
        error.message = data.message
      }
    } else {
      console.error(data, error.request)
    }

    // 挂载服务端返回信息
    error.data = data || {}

    throw error
  },
)
export default axios$
