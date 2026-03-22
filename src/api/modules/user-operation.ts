import axios from '@/api/axios'

export const userOperationApi = {
  record(data: {
    requester: string
    feishuId: string
    flowType: string
    stepName: string
    action: string
    status: string
    payload?: Record<string, any>
  }) {
    return axios.post<boolean>('/v1/supply/task/user-operation/record', data)
  },
  list(params: {
    pageNo: number
    pageSize: number
    requester?: string
    feishuId?: string
    flowType?: string
    status?: string
    createdAt?: string
  }) {
    return axios.get<{ list: Array<Record<string, any>>; total: number }>('/v1/supply/task/user-operation/list', { params })
  },
}
