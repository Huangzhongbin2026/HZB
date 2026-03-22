import axios from '@/api/axios'
import type { DeliveryAnalyzeResponse, DeliveryManagementItem } from '@/types/supply-task/delivery-change'

export const deliveryChangeApi = {
  generateSignature(data: { sys_id: string; access_key_secret: string }) {
    return axios.post<{ signature: string }>('/api/v1/supply/task/delivery-change/signature', data)
  },
  analyzeStep1(data: { contractNo: string; taskType: string; aiPayload?: Record<string, string> }, headers: { sysId: string; signServerAuth: string }) {
    return axios.post<DeliveryAnalyzeResponse>('/api/v1/supply/task/delivery-change/step1/analyze', data, {
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  duplicateCheck(contractNo: string, taskType: string) {
    return axios.get<{ duplicated: boolean }>('/api/v1/supply/task/delivery-change/duplicate-check', { params: { contractNo, taskType } })
  },
  queryPlan(crmNumber: string, headers: { sysId: string; signServerAuth: string }) {
    return axios.get<{ 盘点记录: Array<Record<string, any>>; 项目盘点详情url: string }>('/api/v1/supply/task/delivery-change/step3/plan', {
      params: { crmNumber },
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  submit(data: Record<string, any>, headers: { sysId: string; signServerAuth: string }) {
    return axios.post<{ approvalNo: string; count: number }>('/api/v1/supply/task/delivery-change/submit', data, {
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  queryManagement(params: {
    pageNo: number
    pageSize: number
    approvalNo?: string
    contractNo?: string
    taskType?: string
    createdAt?: string
  }) {
    return axios.get<{ list: DeliveryManagementItem[]; total: number }>('/api/v1/supply/task/delivery-change/management', { params })
  },
  saveEvaluation(approvalNo: string, marketCode: string, reply: string) {
    return axios.post<boolean>(`/api/v1/supply/task/delivery-change/management/${approvalNo}/evaluation`, { marketCode, reply })
  },
  urgeUser(approvalNo: string) {
    return axios.post<boolean>(`/api/v1/supply/task/delivery-change/management/${approvalNo}/urge-user`)
  },
  urgeRegion(approvalNo: string) {
    return axios.post<boolean>(`/api/v1/supply/task/delivery-change/management/${approvalNo}/urge-region`)
  },
}
