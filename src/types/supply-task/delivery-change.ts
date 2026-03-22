export interface DeliveryAnalyzeResponse {
  contractNo: string
  taskType: '客期提前' | '客期延后'
  orderData: Array<Record<string, any>>
  productArray: Array<Record<string, any>>
  allExpectAfterToday: boolean
  maxExpectDate: string
  minExpectDate: string
  extracted: {
    crmNumber: string
    projectName: string
    salesDept: string
    region: string
  }
  changeReason: string
  targetDeliveryDate: string
  allowPartialShipmentIfIncomplete: string
}

export interface DeliveryManagementItem extends Record<string, any> {
  审批编号: string
  任务创建时间: string
  合同编号: string
  任务类型: string
  市场代码名称: string
  统筹评估回复: string
  任务评估状态: string
  任务完成时间: string
}
