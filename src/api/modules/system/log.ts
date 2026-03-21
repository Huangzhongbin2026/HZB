import axios from '@/api/axios'
import type { SysOperationLog } from '@/types/system'

export interface LogQuery {
  pageNo: number
  pageSize: number
  operUser?: string
  operType?: string
  operModule?: string
  operIp?: string
  keyword?: string
  startTime?: string
  endTime?: string
}

export const systemLogApi = {
  query(params: LogQuery) {
    return axios.get<{ list: SysOperationLog[]; total: number }>('/api/v1/supply/system/logs', { params })
  },
  export(params: LogQuery) {
    return axios.get('/api/v1/supply/system/logs/export', { params, responseType: 'blob' })
  },
  clean(startTime: string, endTime: string) {
    return axios.post<boolean>('/api/v1/supply/system/logs/clean', { startTime, endTime })
  },
  record(payload: Record<string, unknown>) {
    return axios.post<boolean>('/api/v1/supply/system/logs/record', payload)
  },
}
