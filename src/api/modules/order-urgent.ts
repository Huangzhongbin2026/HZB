import axios from '@/api/axios'
import type { OrderUrgentAnalyzeResponse, OrderUrgentManagementItem } from '@/types/supply-task/order-urgent'

export const orderUrgentApi = {
  generateSignature(data: { sys_id: string; access_key_secret: string }) {
    return axios.post<{ signature: string }>('/api/v1/supply/task/order-urgent/signature', data)
  },
  analyzeStep1(
    data: { contractNo: string; aiPayload?: Record<string, string> },
    headers: { sysId: string; signServerAuth: string },
  ) {
    return axios.post<OrderUrgentAnalyzeResponse>('/api/v1/supply/task/order-urgent/step1/analyze', data, {
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  duplicateCheck(contractNo: string) {
    return axios.get<{ duplicated: boolean }>('/api/v1/supply/task/order-urgent/duplicate-check', { params: { contractNo } })
  },
  queryPlan(crmNumber: string, headers: { sysId: string; signServerAuth: string }) {
    return axios.post<{ 盘点记录: Array<Record<string, any>>; 项目盘点详情url: string }>('/api/v1/supply/task/order-urgent/step3/plan', {
      crmNumber,
    }, {
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  submit(data: Record<string, any>, headers: { sysId: string; signServerAuth: string; feishuAuthorization?: string }) {
    return axios.post<{ taskNo: string; contractNo: string; followResult: string }>('/api/v1/supply/task/order-urgent/submit', data, {
      headers: {
        sysId: headers.sysId,
        'sign-server-auth': headers.signServerAuth,
        'Feishu-Authorization': headers.feishuAuthorization || '',
      },
    })
  },
  queryManagement(params: {
    pageNo: number
    pageSize: number
    contractNo?: string
    orderNo?: string
    projectName?: string
    createdAt?: string
  }) {
    return axios.get<{ list: OrderUrgentManagementItem[]; total: number }>('/api/v1/supply/task/order-urgent/management', {
      params,
    })
  },
  saveEvaluation(taskNo: string, reply: string) {
    return axios.post<boolean>(`/api/v1/supply/task/order-urgent/management/${taskNo}/evaluation`, { reply })
  },
  urgeUser(taskNo: string) {
    return axios.post<boolean>(`/api/v1/supply/task/order-urgent/management/${taskNo}/urge-user`)
  },
  urgeRegion(taskNo: string) {
    return axios.post<boolean>(`/api/v1/supply/task/order-urgent/management/${taskNo}/urge-region`)
  },
}
