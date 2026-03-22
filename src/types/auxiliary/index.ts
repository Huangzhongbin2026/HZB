export interface LeaveConfigItem {
  id: string
  userId: string
  userName: string
  leaveStart: string
  leaveEnd: string
  leaveReason?: string
  remark?: string
  status: boolean
}

export interface VirtualProductItem {
  id: string
  productModel: string
  autoReplyContent: string
  status: boolean
  createdAt?: string
}

export interface MessagePushItem {
  id: string
  pushName: string
  routeCode: string
  feishuTemplateCode?: string
  isEnabled: boolean
  pushRule?: string
  createdAt?: string
}

export interface LeaveAgentProductItem {
  id: string
  productModel: string
  originalUserId: string
  originalUserName: string
  agentUserId: string
  agentUserName: string
  status: boolean
}

export interface AreaCoordinatorItem {
  id: string
  saleDeptCode: string
  provinceCode: string
  region?: string
  deptKeyword?: string
  projectKeyword?: string
  coordinatorUserId: string
  coordinatorUserName: string
  agentCoordinatorUserId: string
  agentCoordinatorUserName: string
  createdAt?: string
  priorityNo: number
  status: boolean
}

export interface PageResponse<T> {
  list: T[]
  total: number
}
