import type { AreaCoordinatorItem, LeaveAgentProductItem, VirtualProductItem } from '@/types/auxiliary'

export const matchVirtualProduct = (list: VirtualProductItem[], model: string) =>
  list.find((item) => item.productModel === model)

export const matchLeaveAgent = (list: LeaveAgentProductItem[], model: string, originalUserId: string) =>
  list.find((item) => item.productModel === model && item.originalUserId === originalUserId)

export const matchAreaCoordinator = (
  list: AreaCoordinatorItem[],
  saleDeptCode: string,
  provinceCode: string,
  deptKeyword: string,
  projectKeyword: string,
) => {
  const precise = list
    .filter((item) => item.saleDeptCode === saleDeptCode && item.provinceCode === provinceCode)
    .sort((a, b) => a.priorityNo - b.priorityNo)
  if (precise.length) {
    const keywordHit = precise.find(
      (item) =>
        (!item.deptKeyword || deptKeyword.includes(item.deptKeyword)) &&
        (!item.projectKeyword || projectKeyword.includes(item.projectKeyword)),
    )
    return keywordHit || precise[0]
  }

  return list.find(
    (item) =>
      (!!item.deptKeyword && deptKeyword.includes(item.deptKeyword)) ||
      (!!item.projectKeyword && projectKeyword.includes(item.projectKeyword)),
  )
}
