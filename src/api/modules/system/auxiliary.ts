import axios from '@/api/axios'
import type {
  AreaCoordinatorItem,
  LeaveAgentProductItem,
  LeaveConfigItem,
  MessagePushItem,
  PageResponse,
  VirtualProductItem,
} from '@/types/auxiliary'

export const auxiliaryApi = {
  queryLeaveConfigs(params: Record<string, unknown>) {
    return axios.get<PageResponse<LeaveConfigItem>>('/api/v1/supply/system/aux/leave-configs', { params })
  },
  saveLeaveConfig(data: Partial<LeaveConfigItem>) {
    return axios.post<boolean>('/api/v1/supply/system/aux/leave-configs', data)
  },
  updateLeaveConfig(id: string, data: Partial<LeaveConfigItem>) {
    return axios.put<boolean>(`/api/v1/supply/system/aux/leave-configs/${id}`, data)
  },
  deleteLeaveConfig(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/aux/leave-configs/${id}`)
  },
  importLeaveConfigs(data: LeaveConfigItem[]) {
    return axios.post<boolean>('/api/v1/supply/system/aux/leave-configs/import', data)
  },
  exportLeaveConfigs(params: Record<string, unknown>) {
    return axios.get('/api/v1/supply/system/aux/leave-configs/export', { params, responseType: 'blob' })
  },
  matchLeaveByUser(userId: string, date: string) {
    return axios.get<{ onLeave: boolean }>('/api/v1/supply/system/aux/leave-configs/match/by-user', { params: { userId, date } })
  },

  queryVirtualProducts(params: Record<string, unknown>) {
    return axios.get<PageResponse<VirtualProductItem>>('/api/v1/supply/system/aux/virtual-products', { params })
  },
  saveVirtualProduct(data: Partial<VirtualProductItem>) {
    return axios.post<boolean>('/api/v1/supply/system/aux/virtual-products', data)
  },
  updateVirtualProduct(id: string, data: Partial<VirtualProductItem>) {
    return axios.put<boolean>(`/api/v1/supply/system/aux/virtual-products/${id}`, data)
  },
  deleteVirtualProduct(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/aux/virtual-products/${id}`)
  },
  importVirtualProducts(data: VirtualProductItem[]) {
    return axios.post<boolean>('/api/v1/supply/system/aux/virtual-products/import', data)
  },
  exportVirtualProducts(params: Record<string, unknown>) {
    return axios.get('/api/v1/supply/system/aux/virtual-products/export', { params, responseType: 'blob' })
  },
  matchVirtualProduct(productModel: string) {
    return axios.get<{ autoReplyContent: string }>('/api/v1/supply/system/aux/virtual-products/match', { params: { productModel } })
  },

  queryMessagePushes(params: Record<string, unknown>) {
    return axios.get<PageResponse<MessagePushItem>>('/api/v1/supply/system/aux/message-pushes', { params })
  },
  saveMessagePush(data: Partial<MessagePushItem>) {
    return axios.post<boolean>('/api/v1/supply/system/aux/message-pushes', data)
  },
  updateMessagePush(id: string, data: Partial<MessagePushItem>) {
    return axios.put<boolean>(`/api/v1/supply/system/aux/message-pushes/${id}`, data)
  },
  deleteMessagePush(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/aux/message-pushes/${id}`)
  },
  importMessagePushes(data: MessagePushItem[]) {
    return axios.post<boolean>('/api/v1/supply/system/aux/message-pushes/import', data)
  },
  exportMessagePushes(params: Record<string, unknown>) {
    return axios.get('/api/v1/supply/system/aux/message-pushes/export', { params, responseType: 'blob' })
  },

  queryLeaveAgentProducts(params: Record<string, unknown>) {
    return axios.get<PageResponse<LeaveAgentProductItem>>('/api/v1/supply/system/aux/leave-agent-products', { params })
  },
  saveLeaveAgentProduct(data: Partial<LeaveAgentProductItem>) {
    return axios.post<boolean>('/api/v1/supply/system/aux/leave-agent-products', data)
  },
  updateLeaveAgentProduct(id: string, data: Partial<LeaveAgentProductItem>) {
    return axios.put<boolean>(`/api/v1/supply/system/aux/leave-agent-products/${id}`, data)
  },
  deleteLeaveAgentProduct(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/aux/leave-agent-products/${id}`)
  },
  importLeaveAgentProducts(data: LeaveAgentProductItem[]) {
    return axios.post<boolean>('/api/v1/supply/system/aux/leave-agent-products/import', data)
  },
  exportLeaveAgentProducts(params: Record<string, unknown>) {
    return axios.get('/api/v1/supply/system/aux/leave-agent-products/export', { params, responseType: 'blob' })
  },
  matchLeaveAgentProduct(productModel: string, originalUserId: string) {
    return axios.get<{ agentUserId: string; agentUserName: string }>('/api/v1/supply/system/aux/leave-agent-products/match', {
      params: { productModel, originalUserId },
    })
  },

  queryAreaCoordinators(params: Record<string, unknown>) {
    return axios.get<PageResponse<AreaCoordinatorItem>>('/api/v1/supply/system/aux/area-coordinators', { params })
  },
  saveAreaCoordinator(data: Partial<AreaCoordinatorItem>) {
    return axios.post<boolean>('/api/v1/supply/system/aux/area-coordinators', data)
  },
  updateAreaCoordinator(id: string, data: Partial<AreaCoordinatorItem>) {
    return axios.put<boolean>(`/api/v1/supply/system/aux/area-coordinators/${id}`, data)
  },
  deleteAreaCoordinator(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/aux/area-coordinators/${id}`)
  },
  importAreaCoordinators(data: AreaCoordinatorItem[]) {
    return axios.post<boolean>('/api/v1/supply/system/aux/area-coordinators/import', data)
  },
  exportAreaCoordinators(params: Record<string, unknown>) {
    return axios.get('/api/v1/supply/system/aux/area-coordinators/export', { params, responseType: 'blob' })
  },
  matchAreaCoordinator(params: Record<string, unknown>) {
    return axios.get<{ coordinatorUserId: string; coordinatorUserName: string; agentCoordinatorUserId: string; agentCoordinatorUserName: string }>('/api/v1/supply/system/aux/area-coordinators/match', { params })
  },
}
