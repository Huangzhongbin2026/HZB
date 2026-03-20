import axios from '@/api/axios'

export interface LeaveConfig {
  userId: string
  startAt: string
  endAt: string
  agentUserId: string
  productModels: string[]
}

export interface MessageSwitchConfig {
  taskCreated: boolean
  taskTransfer: boolean
  taskOverdue: boolean
}

export const configApi = {
  getLeaveConfigs() {
    return axios.get<LeaveConfig[]>('/api/v1/supply/task/configs/leave')
  },
  saveLeaveConfig(data: LeaveConfig) {
    return axios.put<boolean>('/api/v1/supply/task/configs/leave', data)
  },
  getMessageSwitch() {
    return axios.get<MessageSwitchConfig>('/api/v1/supply/task/configs/message-switch')
  },
  saveMessageSwitch(data: MessageSwitchConfig) {
    return axios.put<boolean>('/api/v1/supply/task/configs/message-switch', data)
  },
}
