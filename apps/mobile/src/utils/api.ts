import { API_BASE } from './config'

const tokenStorageKey = 'knowledge-cloud-mobile-token'

let token = uni.getStorageSync(tokenStorageKey) || ''

export function setToken(nextToken: string) {
  token = nextToken
  uni.setStorageSync(tokenStorageKey, nextToken)
}

export function clearToken() {
  token = ''
  uni.removeStorageSync(tokenStorageKey)
}

export function getToken() {
  return token
}

export function request<T>(url: string, method = 'GET', data?: Record<string, unknown>): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE}${url}`,
      method,
      data,
      header: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {},
      success(response) {
        if (response.statusCode && response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T)
          return
        }
        reject(response)
      },
      fail(error) {
        reject(error)
      },
    })
  })
}

export function uploadFile<T>(url: string, filePath: string, formData: Record<string, string> = {}): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE}${url}`,
      filePath,
      name: 'file',
      formData,
      header: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {},
      success(response) {
        if (response.statusCode && response.statusCode >= 200 && response.statusCode < 300) {
          resolve(JSON.parse(response.data) as T)
          return
        }
        reject(response)
      },
      fail(error) {
        reject(error)
      },
    })
  })
}