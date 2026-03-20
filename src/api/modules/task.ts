import axios from '@/api/axios'
import type { DashboardStat, TaskItem, TaskSubmitDTO } from '@/types/supply-task/task'

interface ApiPage<T> {
  list: T[]
  total: number
}

export interface TaskQuery {
  pageNo: number
  pageSize: number
  keyword?: string
  type?: string
  status?: string
}

export const taskApi = {
  createTask(data: TaskSubmitDTO) {
    return axios.post<{ taskId: string }>('/api/v1/supply/task/tasks', data)
  },
  queryTasks(params: TaskQuery) {
    return axios.get<ApiPage<TaskItem>>('/api/v1/supply/task/tasks', { params })
  },
  getTaskDetail(id: string) {
    return axios.get<TaskItem>(`/api/v1/supply/task/tasks/${id}`)
  },
  transferTask(id: string, targetUserId: string) {
    return axios.post<boolean>(`/api/v1/supply/task/tasks/${id}/transfer`, { targetUserId })
  },
  closeTask(id: string, reason: string) {
    return axios.post<boolean>(`/api/v1/supply/task/tasks/${id}/close`, { reason })
  },
  dashboard() {
    return axios.get<DashboardStat>('/api/v1/supply/task/reports/dashboard')
  },
}
