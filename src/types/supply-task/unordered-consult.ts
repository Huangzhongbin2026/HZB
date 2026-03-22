export interface UnorderedProductItem {
  selected?: boolean
  CRM编号?: string
  产品型号: string
  产品名称?: string
  数量: number
  要求发货日期?: string
  是否备货?: string
  勾选或取消备货时间?: string
  项目盘点详情url?: string
  一级部门?: string
  三级部门?: string
  机会所有人?: string
  项目名称?: string
  客户名称?: string
  是否退市报备?: string
  产品流速?: string
  产品统筹?: string
}

export interface UnorderedAnalyzeResponse {
  judge: 'CRM' | 'PRODUCT'
  crmNumber: string
  needTime: string
  remark: string
  productList: Array<{ 产品型号: string; 数量: number }>
}

export interface UnorderedManagementItem extends Record<string, any> {
  交期咨询任务编号: string
  CRM编号: string
  产品型号: string
  数量: number
  客户期望日期: string
  需求日期: string
  任务创建时间: string
  产品统筹: string
  代理产品统筹: string
  任务评估状态: string
  统筹评估回复: string
  任务完成时间: string
  任务修改时间: string
  任务完成时效: string
}
