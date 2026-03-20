export type TaskType = 'ORDER_URGENT' | 'UNORDERED_ASSESS' | 'DELIVERY_CHANGE'

export type TaskStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'WAIT_CONFIRM'
  | 'DONE'
  | 'CLOSED'

export interface TaskItem {
  id: string
  type: TaskType
  status: TaskStatus
  title: string
  orderNo?: string
  productModel: string
  requester: string
  coordinator: string
  dueAt: string
  priority: number
  createdAt: string
}

export interface TaskSubmitDTO {
  type: TaskType
  title: string
  orderNo?: string
  productModel: string
  requester: string
  requiredDate: string
  reason: string
  priority: number
}

export interface DashboardStat {
  total: number
  pending: number
  overdue: number
  onTimeRate: number
}
