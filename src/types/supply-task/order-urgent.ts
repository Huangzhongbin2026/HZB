export interface OrderUrgentAnalyzeResponse {
  contractNo: string
  orderType: string
  orderData: Array<Record<string, any>>
  orderPlaceDateList: string[]
  expectDeliveryDateList: string[]
  agreedDeliveryOdcList: string[]
  odcEmptyStatus: '全部为空' | '全部不为空' | '部分为空'
  hasTodayOrder: boolean
  emptySearchResult: Array<{ orderNum: string; lineNumber: string; reason: string }>
  extracted: Record<string, any>
  productArray: Array<Record<string, any>>
  projectUrgency: string
  latestArrivalTime: string
  acceptPartialShipment: string
  mostUrgentProductList: string
  delayedDeliveryImpact: string
  coordinator: {
    name: string
    feishuId: string
    agentName: string
  }
}

export interface OrderUrgentManagementItem extends Record<string, any> {
  加急任务编号: string
  合同编号: string
  订单编号: string
  项目名称: string
  任务创建时间: string
  统筹评估回复: string
  任务评估状态: string
}
