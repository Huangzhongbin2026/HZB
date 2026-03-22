import axios from '@/api/axios'
import type { UnorderedAnalyzeResponse, UnorderedManagementItem } from '@/types/supply-task/unordered-consult'

export const unorderedConsultApi = {
  generateSignature(data: { sys_id: string; access_key_secret: string }) {
    return axios.post<{ signature: string }>('/api/v1/supply/task/unordered-consult/signature', data)
  },
  analyzeStep1(data: { crmNo: string; productInfo: string; needTime: string; remark: string }, headers: { sysId: string; signServerAuth: string }) {
    return axios.post<UnorderedAnalyzeResponse>('/api/v1/supply/task/unordered-consult/step1/analyze', data, {
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  queryPlan(crmNumber: string, headers: { sysId: string; signServerAuth: string }) {
    return axios.get<{ 盘点记录: Array<Record<string, any>>; 项目盘点详情url: string; tableData: Array<Record<string, any>> }>(
      '/api/v1/supply/task/unordered-consult/step2/plan',
      {
        params: { crmNumber },
        headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
      },
    )
  },
  duplicateToday(crmNumber: string) {
    return axios.get<{ hasDuplicate: boolean; rows: Array<Record<string, any>> }>('/api/v1/supply/task/unordered-consult/duplicate/today', {
      params: { crmNumber },
    })
  },
  joinTodayDuplicate(crmNumber: string, collaborator: string) {
    return axios.post<boolean>('/api/v1/supply/task/unordered-consult/duplicate/today/join', { crmNumber, collaborator })
  },
  duplicateHistory(crmNumber: string) {
    return axios.get<{ hasHistoryDuplicate: boolean }>('/api/v1/supply/task/unordered-consult/duplicate/history', { params: { crmNumber } })
  },
  submit(data: Record<string, any>, headers: { sysId: string; signServerAuth: string }) {
    return axios.post<{ taskNo: string; count: number }>('/api/v1/supply/task/unordered-consult/submit', data, {
      headers: { sysId: headers.sysId, 'sign-server-auth': headers.signServerAuth },
    })
  },
  queryManagement(params: { pageNo: number; pageSize: number; taskNo?: string; crmNumber?: string; productModel?: string; createdAt?: string }) {
    return axios.get<{ list: UnorderedManagementItem[]; total: number }>('/api/v1/supply/task/unordered-consult/management', { params })
  },
  saveEvaluation(taskNo: string, productModel: string, reply: string, quantity?: number) {
    return axios.post<boolean>(`/api/v1/supply/task/unordered-consult/management/${taskNo}/evaluation`, {
      productModel,
      reply,
      quantity,
    })
  },
  urge(taskNo: string) {
    return axios.post<boolean>(`/api/v1/supply/task/unordered-consult/management/${taskNo}/urge`)
  },
}
