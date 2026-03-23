<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { smartParseTask, type UserTaskType } from '@/services/ai/task-intelligence'
import { orderUrgentApi } from '@/api/modules/order-urgent'
import type { OrderUrgentAnalyzeResponse } from '@/types/supply-task/order-urgent'
import { unorderedConsultApi } from '@/api/modules/unordered-consult'
import type { UnorderedAnalyzeResponse, UnorderedProductItem } from '@/types/supply-task/unordered-consult'
import { deliveryChangeApi } from '@/api/modules/delivery-change'
import type { DeliveryAnalyzeResponse } from '@/types/supply-task/delivery-change'
import { userOperationApi } from '@/api/modules/user-operation'

interface UserProfile {
  id: string
  name: string
  avatarText: string
  feishuOpenId?: string
}

interface TaskDraft {
  type: UserTaskType
  title: string
  rawInput: string
  payload: Record<string, string>
  createdAt: string
}

interface OperationRecord {
  createdAt: string
  flowType: string
  stepName: string
  action: string
  status: string
  requester: string
  feishuId: string
}

type ApiEnvelope<T> = {
  code?: number | string
  message?: string
  data?: T
}

const TASK_TYPE_LABEL: Record<UserTaskType, string> = {
  ORDER_URGENT: '订单加急任务',
  UNORDERED_ASSESS: '未下单咨询任务',
  DELIVERY_CHANGE: '客期变更任务',
}

const defaultUser = reactive<UserProfile>({
  id: 'u-hzb-001',
  name: '黄忠彬',
  avatarText: '头像',
  feishuOpenId: 'ou_9e45b18b0814fbef6b59d9f3f259bdb6',
})

const currentTaskType = ref<UserTaskType | null>(null)
const aiInput = ref('')
const parsing = ref(false)
const stepLoading = ref(false)

const authConfig = reactive({
  sys_id: '93c44ed00760464dbe8c4cd67dec2892',
  access_key_secret: 'F34E2AAC74B645B9A2BAEE47C690A2E9',
  feishuAuthorization: '',
})

const step1ContractNo = ref('')
const stepNo = ref(1)
const step2Data = ref<OrderUrgentAnalyzeResponse | null>(null)
const planData = ref<{ 盘点记录: Array<Record<string, any>>; 项目盘点详情url: string } | null>(null)
const duplicateFlag = ref('否')
const signServerAuth = ref('')
const urgentForm = reactive({
  projectUrgency: '一般',
  latestArrivalTime: '',
  acceptPartialShipment: '否',
  mostUrgentProductList: '',
  delayedDeliveryImpact: '',
})

const unorderedStepNo = ref(1)
const unorderedForm = reactive({
  crmNo: '',
  productInfo: '',
  needTime: '',
  remark: '',
})
const unorderedAnalyze = ref<UnorderedAnalyzeResponse | null>(null)
const unorderedPlanRows = ref<UnorderedProductItem[]>([])
const unorderedRepeatFlag = ref('否')
const unorderedTaskTypeFlag = ref('普通项目')
const todayDuplicateVisible = ref(false)
const todayDuplicateRows = ref<Array<Record<string, any>>>([])
const historyDuplicateVisible = ref(false)

const deliveryStepNo = ref(1)
const deliveryForm = reactive({
  contractNo: '',
  taskType: '客期提前',
  batchTargetDate: '',
  changeReason: '',
  allowPartialShipment: '',
  delayReason: '',
  delayProof: '',
})
const deliveryAnalyze = ref<DeliveryAnalyzeResponse | null>(null)
const deliveryPlan = ref<{ 盘点记录: Array<Record<string, any>>; 项目盘点详情url: string } | null>(null)
const deliveryRepeatFlag = ref('否')

const activeTask = ref<TaskDraft | null>(null)
const historyList = ref<TaskDraft[]>([])
const operationRecords = ref<OperationRecord[]>([])

const flowLabelMap: Record<string, string> = {
  ORDER_URGENT: '订单加急',
  UNORDERED_ASSESS: '未下单咨询',
  DELIVERY_CHANGE: '客期变更',
  CHAT: '智能问答',
}

const isApiSuccess = <T,>(res: ApiEnvelope<T> | null | undefined): res is ApiEnvelope<T> & { data: T } => {
  const success = res?.code === 0 || res?.code === '0'
  return Boolean(success && res?.data != null)
}

const getApiErrorMessage = (res: ApiEnvelope<unknown> | null | undefined, fallback: string) => {
  const message = typeof res?.message === 'string' ? res.message.trim() : ''
  return message || fallback
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

const appendTraceId = (message: string, traceId: unknown) => {
  const trace = String(traceId || '').trim()
  if (!trace) {
    return message
  }
  return `${message}（traceId: ${trace}）`
}

const requestOrderUrgentSignature = async () => {
  let lastRes: ApiEnvelope<{ signature?: string }> | null = null
  for (let i = 0; i < 2; i += 1) {
    const signRes = await orderUrgentApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    lastRes = signRes
    if (isApiSuccess(signRes) && signRes.data.signature) {
      return signRes.data.signature
    }
    if (i === 0) {
      await sleep(150)
    }
  }
  throw new Error(appendTraceId(getApiErrorMessage(lastRes, '签名生成失败，请稍后重试'), (lastRes as any)?.traceId))
}

const normalizeOrderUrgentAnalyzeData = (raw: OrderUrgentAnalyzeResponse) => ({
  ...raw,
  extracted: raw?.extracted || {},
  orderData: Array.isArray(raw?.orderData) ? raw.orderData : [],
  orderPlaceDateList: Array.isArray(raw?.orderPlaceDateList) ? raw.orderPlaceDateList : [],
  expectDeliveryDateList: Array.isArray(raw?.expectDeliveryDateList) ? raw.expectDeliveryDateList : [],
  agreedDeliveryOdcList: Array.isArray(raw?.agreedDeliveryOdcList) ? raw.agreedDeliveryOdcList : [],
  emptySearchResult: Array.isArray(raw?.emptySearchResult) ? raw.emptySearchResult : [],
  productArray: Array.isArray(raw?.productArray) ? raw.productArray : [],
  coordinator: raw?.coordinator || {
    name: '',
    feishuId: '',
    agentName: '',
  },
})

const saveOperationRecord = async (
  flowType: string,
  stepName: string,
  action: string,
  status: 'processing' | 'success' | 'fail',
  payload: Record<string, any> = {},
) => {
  const record: OperationRecord = {
    createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
    flowType,
    stepName,
    action,
    status,
    requester: defaultUser.name,
    feishuId: defaultUser.feishuOpenId || '',
  }
  operationRecords.value.unshift(record)
  if (operationRecords.value.length > 30) {
    operationRecords.value.length = 30
  }
  try {
    await userOperationApi.record({
      requester: defaultUser.name,
      feishuId: defaultUser.feishuOpenId || '',
      flowType,
      stepName,
      action,
      status,
      payload,
    })
  } catch {
    // 本地界面记录优先，后端写入失败不阻塞主流程
  }
}

const welcomeText = `亲~我是锐小蜜，很高兴为您服务！\n\n当前可处理以下订单履约场景：\n1、已下单交期查询/加急\n2、未下单交期咨询\n3、客期变更任务\n\n您可以在左侧手动选择任务类型，或直接输入任务描述进行智能识别。\n如需加速推进，可在流程中点击“任务催办”。\n查看已提交任务，请点击下方“任务管理平台”。`

const summaryTitle = (task: TaskDraft) => {
  if (task.type === 'ORDER_URGENT') {
    const contractNo = task.payload.contractNo || '未识别合同号'
    const project = task.payload.projectName || task.payload.delayedDeliveryImpact || ''
    return `订单加急任务 ${contractNo} ${project}`.trim()
  }

  if (task.type === 'UNORDERED_ASSESS') {
    const crmNo = task.payload.crmNo || ''
    const model = task.payload.productInformation || ''
    const project = task.payload.projectName || ''
    return crmNo
      ? `未下单咨询任务 ${crmNo} ${project}`.trim()
      : `未下单咨询任务 ${model}`.trim()
  }

  const contractNo = task.payload.contractNo || '未识别合同号'
  const project = task.payload.projectName || task.payload.changeReason || ''
  return `客期变更任务 ${contractNo} ${project}`.trim()
}

const historyView = computed(() =>
  historyList.value.map((item) => ({
    ...item,
    title: summaryTitle(item),
  })),
)

const startNewTask = () => {
  currentTaskType.value = null
  activeTask.value = null
  aiInput.value = ''
  stepNo.value = 1
  step1ContractNo.value = ''
  step2Data.value = null
  planData.value = null
  duplicateFlag.value = '否'
  unorderedStepNo.value = 1
  unorderedAnalyze.value = null
  unorderedPlanRows.value = []
  unorderedRepeatFlag.value = '否'
  unorderedTaskTypeFlag.value = '普通项目'
  Object.assign(unorderedForm, { crmNo: '', productInfo: '', needTime: '', remark: '' })
  deliveryStepNo.value = 1
  deliveryAnalyze.value = null
  deliveryPlan.value = null
  deliveryRepeatFlag.value = '否'
  Object.assign(deliveryForm, {
    contractNo: '',
    taskType: '客期提前',
    batchTargetDate: '',
    changeReason: '',
    allowPartialShipment: '',
    delayReason: '',
    delayProof: '',
  })
}

const manualStart = (type: UserTaskType) => {
  currentTaskType.value = type
  if (type === 'ORDER_URGENT') {
    stepNo.value = 1
  }
  if (type === 'UNORDERED_ASSESS') {
    unorderedStepNo.value = 1
  }
  if (type === 'DELIVERY_CHANGE') {
    deliveryStepNo.value = 1
  }
  activeTask.value = {
    type,
    title: TASK_TYPE_LABEL[type],
    rawInput: '',
    payload: {},
    createdAt: new Date().toISOString(),
  }
}

const openTask = (task: TaskDraft) => {
  currentTaskType.value = task.type
  activeTask.value = { ...task }
}

const submitAiInput = async () => {
  const raw = aiInput.value.trim()
  if (!raw) {
    ElMessage.warning('请输入任务描述')
    return
  }

  parsing.value = true
  await saveOperationRecord('CHAT', '输入解析', '智能解析请求', 'processing', { raw })
  try {
    const parsed = await smartParseTask(raw)
    if (!parsed) {
      await saveOperationRecord('CHAT', '输入解析', '智能解析请求', 'fail', { raw })
      ElMessage.error('未识别到有效任务，请补充合同编号或CRM编号后重试')
      return
    }

    const draft: TaskDraft = {
      type: parsed.taskType,
      title: TASK_TYPE_LABEL[parsed.taskType],
      rawInput: raw,
      payload: parsed.payload,
      createdAt: new Date().toISOString(),
    }

    currentTaskType.value = parsed.taskType
    activeTask.value = draft
    if (parsed.taskType === 'ORDER_URGENT' && parsed.payload.contractNo) {
      step1ContractNo.value = parsed.payload.contractNo.replace(/\s+/g, '')
      Object.assign(urgentForm, {
        projectUrgency: parsed.payload.projectUrgency || '一般',
        latestArrivalTime: parsed.payload.latestArrivalTime || '',
        acceptPartialShipment: parsed.payload.acceptPartialShipment || '否',
        mostUrgentProductList: parsed.payload.mostUrgentProductList || '',
        delayedDeliveryImpact: parsed.payload.delayedDeliveryImpact || '',
      })
      await submitStep1(true)
    }
    if (parsed.taskType === 'UNORDERED_ASSESS') {
      Object.assign(unorderedForm, {
        crmNo: parsed.payload.crmNo || '',
        productInfo: parsed.payload.productInformation || '',
        needTime: parsed.payload.needTime || '',
        remark: parsed.payload.remarks || '',
      })
    }
    if (parsed.taskType === 'DELIVERY_CHANGE') {
      Object.assign(deliveryForm, {
        contractNo: parsed.payload.contractNo || '',
        taskType: parsed.payload.questionType || '客期提前',
        batchTargetDate: parsed.payload.targetDeliveryDate || '',
        changeReason: parsed.payload.questionType === '客期提前' ? parsed.payload.changeReason || '' : '',
        delayReason: parsed.payload.questionType === '客期延后' ? parsed.payload.changeReason || '' : '',
        allowPartialShipment: parsed.payload.allowPartialShipmentIfIncomplete || '',
      })
      if (deliveryForm.contractNo) {
        await submitDeliveryStep1(true)
      }
    }
    historyList.value.unshift(draft)
    aiInput.value = ''

    await saveOperationRecord(parsed.taskType, '输入解析', '智能解析请求', 'success', { payload: parsed.payload })

    ElMessage.success(`已识别任务类型：${TASK_TYPE_LABEL[parsed.taskType]}，请继续完善流程信息`)
  } catch (error) {
    await saveOperationRecord('CHAT', '输入解析', '智能解析请求', 'fail', { message: String(error) })
    ElMessage.error('智能识别失败，请检查输入内容后重试')
    console.error(error)
  } finally {
    parsing.value = false
  }
}

const goManagement = () => {
  const tab = currentTaskType.value === 'ORDER_URGENT' ? 'urgent' : currentTaskType.value === 'DELIVERY_CHANGE' ? 'delivery' : 'unordered'
  window.open(`/?mode=management&tab=${tab}`, '_blank')
}

const isManualActive = (type: UserTaskType) => currentTaskType.value === type

const consultHuman = () => {
  ElMessage.info('人工咨询功能已预留，后续接入客服系统')
}

const bindFeishuUser = (profile: Partial<UserProfile>) => {
  if (profile.name) {
    defaultUser.name = profile.name
  }
  if (profile.feishuOpenId) {
    defaultUser.feishuOpenId = profile.feishuOpenId
  }
}

defineExpose({ bindFeishuUser })

const selectedProducts = computed(() =>
  (step2Data.value?.productArray || []).filter((item) => item.selected),
)

const submitStep1 = async (fromAi = false) => {
  if (currentTaskType.value !== 'ORDER_URGENT') {
    return
  }
  const contractNo = step1ContractNo.value.replace(/\s+/g, '')
  if (!contractNo) {
    ElMessage.warning('请先填写合同编号')
    return
  }
  stepLoading.value = true
  await saveOperationRecord('ORDER_URGENT', '第一步', '合同分析', 'processing', { contractNo })
  try {
    signServerAuth.value = await requestOrderUrgentSignature()

    let res: ApiEnvelope<OrderUrgentAnalyzeResponse> | null = null
    for (let i = 0; i < 2; i += 1) {
      res = await orderUrgentApi.analyzeStep1(
        {
          contractNo,
          aiPayload: activeTask.value?.payload || {},
        },
        {
          sysId: authConfig.sys_id,
          signServerAuth: signServerAuth.value,
        },
      )

      if (isApiSuccess(res)) {
        break
      }

      if (i === 0) {
        // 系统偶发签名校验或后端瞬时异常时重试一次
        signServerAuth.value = await requestOrderUrgentSignature()
        await sleep(150)
      }
    }

    if (!isApiSuccess(res)) {
      throw new Error(appendTraceId(getApiErrorMessage(res, '订单加急任务解析失败，请检查合同编号后重试'), (res as any)?.traceId))
    }

    step2Data.value = normalizeOrderUrgentAnalyzeData(res.data)
    stepNo.value = 2
    Object.assign(urgentForm, {
      projectUrgency: step2Data.value.projectUrgency || urgentForm.projectUrgency,
      latestArrivalTime: step2Data.value.latestArrivalTime || urgentForm.latestArrivalTime,
      acceptPartialShipment: step2Data.value.acceptPartialShipment || urgentForm.acceptPartialShipment,
      mostUrgentProductList: step2Data.value.mostUrgentProductList || urgentForm.mostUrgentProductList,
      delayedDeliveryImpact: step2Data.value.delayedDeliveryImpact || urgentForm.delayedDeliveryImpact,
    })
    if (!fromAi) {
      historyList.value.unshift({
        type: 'ORDER_URGENT',
        title: '订单加急任务',
        rawInput: contractNo,
        payload: { contractNo },
        createdAt: new Date().toISOString(),
      })
    }
    await saveOperationRecord('ORDER_URGENT', '第一步', '合同分析', 'success', { contractNo })
  } catch (error: any) {
    const message = appendTraceId(error?.data?.message || error?.message || '订单加急任务解析失败，请检查合同编号后重试', error?.data?.traceId)
    await saveOperationRecord('ORDER_URGENT', '第一步', '合同分析', 'fail', { contractNo, message })
    ElMessage.error(message)
  } finally {
    stepLoading.value = false
  }
}

const handleLatestArrivalChange = async (val: string) => {
  if (!val || !step2Data.value) return
  const today = new Date().toISOString().slice(0, 10)
  if (val < today) {
    ElMessage.warning('最迟需要到货时间不能早于当天')
    urgentForm.latestArrivalTime = ''
    return
  }

  const minExpect = (step2Data.value.expectDeliveryDateList || []).filter(Boolean).sort()[0]
  if (minExpect && val < minExpect) {
    try {
      await ElMessageBox.confirm('您输入的时间早于订单当前的客期期望交期，请走客期提前流程！', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      ElMessage.info('客期提前流程待后续对接，当前已取消本次日期选择')
      urgentForm.latestArrivalTime = ''
    } catch {
      urgentForm.latestArrivalTime = ''
    }
    return
  }

  const invalidByOdc = (step2Data.value.productArray || []).some((item) => {
    const odc = String(item['约定交期-ODC'] || '')
    return odc && val >= odc
  })
  if (invalidByOdc) {
    ElMessage.warning('最迟需要到货时间需小于当前约定交期-ODC')
    urgentForm.latestArrivalTime = ''
    return
  }

  ;(step2Data.value.productArray || []).forEach((item) => {
    if (item.selected) {
      item['最迟到货时间'] = val
    }
  })
}

const nextToStep3 = async () => {
  if (!step2Data.value) {
    return
  }

  const duplicate = await orderUrgentApi.duplicateCheck(step2Data.value.contractNo)
  if (duplicate.data.duplicated) {
    try {
      await ElMessageBox.confirm('您输入的合同编号存在已提交的记录，请仔细核对后确定是否继续提交！', '重复提交提醒', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
      duplicateFlag.value = '重复提交'
    } catch {
      return
    }
  } else {
    duplicateFlag.value = '否'
  }

  stepNo.value = 3
  await saveOperationRecord('ORDER_URGENT', '第二步', '重复校验并进入计划查询', 'processing', { contractNo: step2Data.value.contractNo })
  const crm = String(step2Data.value.extracted.crmNumber || '')
  if (crm) {
    const signRes = await orderUrgentApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    signServerAuth.value = signRes.data.signature
    const planRes = await orderUrgentApi.queryPlan(crm, {
      sysId: authConfig.sys_id,
      signServerAuth: signServerAuth.value,
    })
    planData.value = planRes.data
  } else {
    planData.value = { 盘点记录: [], 项目盘点详情url: '' }
  }
  await saveOperationRecord('ORDER_URGENT', '第二步', '重复校验并进入计划查询', 'success', { contractNo: step2Data.value.contractNo })
}

const submitFinal = async () => {
  if (!step2Data.value) return
  if (!selectedProducts.value.length) {
    ElMessage.warning('请至少勾选一条可提交数据')
    return
  }
  const payload = selectedProducts.value.map((item) => ({
    ...item,
    项目紧急性: urgentForm.projectUrgency,
    最迟到货时间: urgentForm.latestArrivalTime,
    是否接受分批发货: urgentForm.acceptPartialShipment,
    最紧急的商品清单: urgentForm.mostUrgentProductList,
    不及时交付的影响: urgentForm.delayedDeliveryImpact,
    区域统筹: step2Data.value?.coordinator?.name || '',
    代理区域统筹: step2Data.value?.coordinator?.agentName || '',
    项目盘点详情url: planData.value?.项目盘点详情url || '',
  }))

  const signRes = await orderUrgentApi.generateSignature({
    sys_id: authConfig.sys_id,
    access_key_secret: authConfig.access_key_secret,
  })
  signServerAuth.value = signRes.data.signature

  const res = await orderUrgentApi.submit({
    contractNo: step2Data.value.contractNo,
    requesterName: defaultUser.name,
    duplicateFlag: duplicateFlag.value,
    regionalCoordinatorFeishuId: step2Data.value?.coordinator?.feishuId || '',
    productArray: payload,
  }, {
    sysId: authConfig.sys_id,
    signServerAuth: signServerAuth.value,
    feishuAuthorization: authConfig.feishuAuthorization,
  })
  stepNo.value = 4
  await saveOperationRecord('ORDER_URGENT', '第三步', '提交任务', 'success', { taskNo: res.data.taskNo })
  ElMessage.success(`提交成功，加急任务编号：${res.data.taskNo}`)
}

const selectedUnorderedRows = computed(() => unorderedPlanRows.value.filter((item) => item.selected))

const submitUnorderedStep1 = async () => {
  if (currentTaskType.value !== 'UNORDERED_ASSESS') {
    return
  }
  if (!unorderedForm.crmNo.trim() && !unorderedForm.productInfo.trim()) {
    ElMessage.warning('请填写CRM编号或产品信息，二选一')
    return
  }
  if (!unorderedForm.needTime || !unorderedForm.remark.trim()) {
    ElMessage.warning('请填写需求时间和备注信息')
    return
  }
  stepLoading.value = true
  await saveOperationRecord('UNORDERED_ASSESS', '第一步', '咨询解析', 'processing', { crmNo: unorderedForm.crmNo, productInfo: unorderedForm.productInfo })
  try {
    const signRes = await unorderedConsultApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    signServerAuth.value = signRes.data.signature

    const analyzeRes = await unorderedConsultApi.analyzeStep1(
      {
        crmNo: unorderedForm.crmNo,
        productInfo: unorderedForm.productInfo,
        needTime: unorderedForm.needTime,
        remark: unorderedForm.remark,
      },
      {
        sysId: authConfig.sys_id,
        signServerAuth: signServerAuth.value,
      },
    )
    unorderedAnalyze.value = analyzeRes.data

    if (analyzeRes.data.judge === 'CRM' && analyzeRes.data.crmNumber) {
      const refreshSign = await unorderedConsultApi.generateSignature({
        sys_id: authConfig.sys_id,
        access_key_secret: authConfig.access_key_secret,
      })
      signServerAuth.value = refreshSign.data.signature
      const planRes = await unorderedConsultApi.queryPlan(analyzeRes.data.crmNumber, {
        sysId: authConfig.sys_id,
        signServerAuth: signServerAuth.value,
      })
      unorderedPlanRows.value = (planRes.data.tableData || []).map((x) => ({ ...x, selected: true } as UnorderedProductItem))
    } else {
      unorderedPlanRows.value = (analyzeRes.data.productList || []).map((x) => ({
        selected: true,
        产品型号: x.产品型号,
        数量: x.数量,
        产品名称: '',
      }))
    }
    unorderedStepNo.value = 2
    await saveOperationRecord('UNORDERED_ASSESS', '第一步', '咨询解析', 'success', { mode: unorderedAnalyze.value?.judge || '' })
  } catch (error: any) {
    await saveOperationRecord('UNORDERED_ASSESS', '第一步', '咨询解析', 'fail', { message: error?.data?.message || '' })
    ElMessage.error(error?.data?.message || '未下单咨询任务解析失败，请检查输入后重试')
  } finally {
    stepLoading.value = false
  }
}

const nextUnorderedStep = async () => {
  if (!unorderedAnalyze.value) {
    return
  }
  if (!selectedUnorderedRows.value.length) {
    ElMessage.warning('请至少勾选一条有效数据')
    return
  }

  if (unorderedAnalyze.value.judge === 'CRM' && unorderedAnalyze.value.crmNumber) {
    const todayRes = await unorderedConsultApi.duplicateToday(unorderedAnalyze.value.crmNumber)
    if (todayRes.data.hasDuplicate) {
      todayDuplicateRows.value = todayRes.data.rows
      todayDuplicateVisible.value = true
      return
    }

    const hisRes = await unorderedConsultApi.duplicateHistory(unorderedAnalyze.value.crmNumber)
    if (hisRes.data.hasHistoryDuplicate) {
      historyDuplicateVisible.value = true
      return
    }
  }

  await submitUnorderedFinal()
}

const joinTodayDuplicate = async () => {
  if (!unorderedAnalyze.value?.crmNumber) {
    return
  }
  await unorderedConsultApi.joinTodayDuplicate(unorderedAnalyze.value.crmNumber, defaultUser.name)
  todayDuplicateVisible.value = false
  ElMessage.success('已加入已评估任务并更新协作人')
  goManagement()
}

const continueTodayDuplicate = async () => {
  todayDuplicateVisible.value = false
  const hisRes = await unorderedConsultApi.duplicateHistory(unorderedAnalyze.value?.crmNumber || '')
  if (hisRes.data.hasHistoryDuplicate) {
    historyDuplicateVisible.value = true
    return
  }
  await submitUnorderedFinal()
}

const submitAgainNormal = async () => {
  unorderedRepeatFlag.value = '是'
  unorderedTaskTypeFlag.value = '普通项目'
  historyDuplicateVisible.value = false
  await submitUnorderedFinal()
}

const submitAgainImportant = async () => {
  unorderedRepeatFlag.value = '是'
  unorderedTaskTypeFlag.value = '重要项目'
  historyDuplicateVisible.value = false
  await submitUnorderedFinal()
}

const submitUnorderedFinal = async () => {
  if (!unorderedAnalyze.value) {
    return
  }
  stepLoading.value = true
  await saveOperationRecord('UNORDERED_ASSESS', '第二步', '提交任务', 'processing', { selectedCount: selectedUnorderedRows.value.length })
  try {
    const signRes = await unorderedConsultApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    signServerAuth.value = signRes.data.signature

    const res = await unorderedConsultApi.submit(
      {
        mode: unorderedAnalyze.value.judge,
        requesterName: defaultUser.name,
        crmNumber: unorderedAnalyze.value.crmNumber,
        needTime: unorderedAnalyze.value.needTime,
        remark: unorderedAnalyze.value.remark,
        taskType: unorderedTaskTypeFlag.value,
        repeatFlag: unorderedRepeatFlag.value,
        selectedData: selectedUnorderedRows.value,
      },
      {
        sysId: authConfig.sys_id,
        signServerAuth: signServerAuth.value,
      },
    )
    unorderedStepNo.value = 3
    await saveOperationRecord('UNORDERED_ASSESS', '第二步', '提交任务', 'success', { taskNo: res.data.taskNo })
    ElMessage.success(`提交成功，交期咨询任务编号：${res.data.taskNo}`)
  } catch (error: any) {
    await saveOperationRecord('UNORDERED_ASSESS', '第二步', '提交任务', 'fail', { message: error?.data?.message || '' })
    ElMessage.error(error?.data?.message || '未下单咨询提交失败，请稍后重试')
  } finally {
    stepLoading.value = false
  }
}

const deliverySelectedRows = computed(() => (deliveryAnalyze.value?.productArray || []).filter((x) => x.selected))

const submitDeliveryStep1 = async (fromAi = false) => {
  if (currentTaskType.value !== 'DELIVERY_CHANGE') {
    return
  }
  const contractNo = deliveryForm.contractNo.replace(/\s+/g, '')
  if (!contractNo) {
    ElMessage.warning('请先填写合同编号')
    return
  }

  stepLoading.value = true
  await saveOperationRecord('DELIVERY_CHANGE', '第一步', '合同分析', 'processing', { contractNo })
  try {
    const signRes = await deliveryChangeApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    signServerAuth.value = signRes.data.signature
    const res = await deliveryChangeApi.analyzeStep1({
      contractNo,
      taskType: deliveryForm.taskType,
      aiPayload: activeTask.value?.payload || {},
    }, {
      sysId: authConfig.sys_id,
      signServerAuth: signServerAuth.value,
    })
    deliveryAnalyze.value = res.data
    deliveryForm.contractNo = res.data.contractNo
    deliveryForm.taskType = res.data.taskType
    if (res.data.changeReason && deliveryForm.taskType === '客期提前') {
      deliveryForm.changeReason = res.data.changeReason
    }
    if (res.data.changeReason && deliveryForm.taskType === '客期延后') {
      deliveryForm.delayReason = res.data.changeReason
    }
    if (res.data.allowPartialShipmentIfIncomplete) {
      deliveryForm.allowPartialShipment = res.data.allowPartialShipmentIfIncomplete
    }
    if (res.data.targetDeliveryDate) {
      deliveryForm.batchTargetDate = res.data.targetDeliveryDate
    }
    deliveryStepNo.value = 2
    if (!fromAi) {
      historyList.value.unshift({
        type: 'DELIVERY_CHANGE',
        title: '客期变更任务',
        rawInput: contractNo,
        payload: { contractNo, questionType: deliveryForm.taskType },
        createdAt: new Date().toISOString(),
      })
    }
    await saveOperationRecord('DELIVERY_CHANGE', '第一步', '合同分析', 'success', { contractNo })
  } catch (error: any) {
    await saveOperationRecord('DELIVERY_CHANGE', '第一步', '合同分析', 'fail', { contractNo, message: error?.data?.message || '' })
    ElMessage.error(error?.data?.message || '客期变更任务解析失败，请检查输入后重试')
  } finally {
    stepLoading.value = false
  }
}

const applyDeliveryBatchDate = (date: string) => {
  if (!deliveryAnalyze.value || !date) {
    return
  }
  const today = new Date().toISOString().slice(0, 10)
  const type = deliveryForm.taskType
  if (type === '客期提前') {
    if (date < today) {
      ElMessage.warning('客期提前日期需大于等于当天，请重新选择')
      deliveryForm.batchTargetDate = ''
      return
    }
    if (deliveryAnalyze.value.maxExpectDate && date >= deliveryAnalyze.value.maxExpectDate) {
      ElMessage.warning('选择的客期需要小于订单数据当中“客户期望交期”的最大值，请重新选择！')
      deliveryForm.batchTargetDate = ''
      return
    }
    ;(deliveryAnalyze.value.productArray || []).forEach((row) => {
      const expect = String(row['客户期望交期'] || '')
      if (row.selected && expect > date) {
        row['客期提前至'] = date
      }
    })
    return
  }

  if (date <= today) {
    ElMessage.warning('客期延后日期需大于当天，请重新选择')
    deliveryForm.batchTargetDate = ''
    return
  }
  if (deliveryAnalyze.value.minExpectDate && date <= deliveryAnalyze.value.minExpectDate) {
    ElMessage.warning('选择的客期需要大于订单数据当中“客户期望交期”的最小值，请重新选择！')
    deliveryForm.batchTargetDate = ''
    return
  }
  ;(deliveryAnalyze.value.productArray || []).forEach((row) => {
    const expect = String(row['客户期望交期'] || '')
    if (row.selected && expect < date) {
      row['客期延后至'] = date
    }
  })
}

const nextDeliveryStep = async () => {
  if (!deliveryAnalyze.value) {
    return
  }
  if (!deliverySelectedRows.value.length) {
    ElMessage.warning('请至少选择一条有效订单数据')
    return
  }
  const deliveryDateField = deliveryForm.taskType === '客期提前' ? '客期提前至' : '客期延后至'
  const hasMissingDate = deliverySelectedRows.value.some((row) => !String(row[deliveryDateField] || '').trim())
  if (hasMissingDate) {
    ElMessage.warning(`请为已勾选行填写${deliveryDateField}`)
    return
  }
  if (deliveryForm.taskType === '客期提前' && !deliveryForm.changeReason.trim()) {
    ElMessage.warning('请填写提前原因')
    return
  }
  if (deliveryForm.taskType === '客期提前' && !deliveryForm.allowPartialShipment) {
    ElMessage.warning('请选择是否同意分批发货')
    return
  }
  if (deliveryForm.taskType === '客期延后' && !deliveryForm.delayReason.trim()) {
    ElMessage.warning('请填写延后原因')
    return
  }
  if (deliveryForm.taskType === '客期延后' && !deliveryForm.delayProof) {
    ElMessage.warning('客期延后需要上传延后证明')
    return
  }

  const duplicate = await deliveryChangeApi.duplicateCheck(deliveryAnalyze.value.contractNo, deliveryForm.taskType)
  if (duplicate.data.duplicated) {
    try {
      await ElMessageBox.confirm('您输入的合同编号存在已提交的记录，请仔细核对后确定是否继续提交！', '重复提交提醒', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
      deliveryRepeatFlag.value = '重复提交'
    } catch {
      return
    }
  } else {
    deliveryRepeatFlag.value = '否'
  }

  const crm = String(deliveryAnalyze.value.extracted.crmNumber || '')
  await saveOperationRecord('DELIVERY_CHANGE', '第二步', '重复校验并进入计划查询', 'processing', { contractNo: deliveryAnalyze.value.contractNo })
  if (crm) {
    const signRes = await deliveryChangeApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    signServerAuth.value = signRes.data.signature
    const planRes = await deliveryChangeApi.queryPlan(crm, {
      sysId: authConfig.sys_id,
      signServerAuth: signServerAuth.value,
    })
    deliveryPlan.value = planRes.data
  } else {
    deliveryPlan.value = { 盘点记录: [], 项目盘点详情url: '' }
  }
  deliveryStepNo.value = 3
  await saveOperationRecord('DELIVERY_CHANGE', '第二步', '重复校验并进入计划查询', 'success', { contractNo: deliveryAnalyze.value.contractNo })
}

const submitDeliveryFinal = async () => {
  if (!deliveryAnalyze.value) {
    return
  }
  stepLoading.value = true
  await saveOperationRecord('DELIVERY_CHANGE', '第三步', '提交任务', 'processing', { selectedCount: deliverySelectedRows.value.length })
  try {
    const signRes = await deliveryChangeApi.generateSignature({
      sys_id: authConfig.sys_id,
      access_key_secret: authConfig.access_key_secret,
    })
    signServerAuth.value = signRes.data.signature
    const productArray = deliverySelectedRows.value.map((row) => ({
      ...row,
      变更数量: row['变更数量'],
      客期提前至: row['客期提前至'] || '',
      客期延后至: row['客期延后至'] || '',
    }))
    const res = await deliveryChangeApi.submit({
      contractNo: deliveryAnalyze.value.contractNo,
      requesterName: defaultUser.name,
      taskType: deliveryForm.taskType,
      repeatFlag: deliveryRepeatFlag.value,
      changeReason: deliveryForm.changeReason,
      delayReason: deliveryForm.delayReason,
      delayProof: deliveryForm.delayProof,
      allowPartialShipment: deliveryForm.allowPartialShipment,
      crmNumber: deliveryAnalyze.value.extracted.crmNumber,
      salesDept: deliveryAnalyze.value.extracted.salesDept,
      projectName: deliveryAnalyze.value.extracted.projectName,
      region: deliveryAnalyze.value.extracted.region,
      projectPlanUrl: deliveryPlan.value?.项目盘点详情url || '',
      productArray,
    }, {
      sysId: authConfig.sys_id,
      signServerAuth: signServerAuth.value,
    })
    ElMessage.success(`提交成功，审批编号：${res.data.approvalNo}`)
    await saveOperationRecord('DELIVERY_CHANGE', '第三步', '提交任务', 'success', { approvalNo: res.data.approvalNo })
    goManagement()
  } catch (error: any) {
    await saveOperationRecord('DELIVERY_CHANGE', '第三步', '提交任务', 'fail', { message: error?.data?.message || '' })
    ElMessage.error(error?.data?.message || '客期变更提交失败，请稍后重试')
  } finally {
    stepLoading.value = false
  }
}

const handleDelayProofChange = (file: any) => {
  deliveryForm.delayProof = file?.name || ''
}
</script>

<template>
  <div class="user-shell">
    <header class="user-topbar">
      <div class="brand-wrap">
        <span class="brand-mark">R</span>
        <span class="brand-name">订单履行AI锐小蜜</span>
      </div>
      <div class="profile">
        <span class="avatar">{{ defaultUser.avatarText }}</span>
        <span class="name">{{ defaultUser.name }}</span>
      </div>
    </header>

    <div class="user-workbench">
      <aside class="left-panel">
        <el-button class="new-task-btn" type="primary" plain @click="startNewTask">
          开启新任务
        </el-button>

        <div class="manual-list">
          <button class="manual-item" :class="{ active: isManualActive('ORDER_URGENT') }" @click="manualStart('ORDER_URGENT')">⊕ 订单加急任务</button>
          <button class="manual-item" :class="{ active: isManualActive('UNORDERED_ASSESS') }" @click="manualStart('UNORDERED_ASSESS')">⊕ 未下单咨询任务</button>
          <button class="manual-item" :class="{ active: isManualActive('DELIVERY_CHANGE') }" @click="manualStart('DELIVERY_CHANGE')">⊕ 客期变更任务</button>
        </div>

        <div class="history-title">任务记录</div>
        <div class="history-list">
          <div
            v-for="(item, index) in historyView"
            :key="`${item.createdAt}-${index}`"
            class="history-item"
            :title="item.title"
            @click="openTask(item)"
          >
            <span class="history-text">{{ item.title }}</span>
            <span class="history-date">{{ item.createdAt.slice(5, 10).replace('-', '.') }}</span>
          </div>
        </div>

        <div class="history-title">操作记录</div>
        <div class="history-list">
          <div
            v-for="(item, idx) in operationRecords.slice(0, 10)"
            :key="`op-${idx}-${item.createdAt}`"
            class="history-item"
          >
            <span class="history-text">{{ flowLabelMap[item.flowType] || item.flowType }} / {{ item.stepName }} / {{ item.requester }} {{ item.feishuId || '-' }}</span>
            <span class="history-date">{{ item.status }}</span>
          </div>
        </div>
      </aside>

      <section class="main-panel">

      <article class="chat-card">
        <template v-if="!activeTask">
          <h2>亲~我是锐小蜜，很高兴为您服务! 🤝</h2>
          <p v-for="(line, idx) in welcomeText.split('\n')" :key="idx" class="welcome-line">
            {{ line }}
          </p>
        </template>

        <template v-else>
          <h2>{{ TASK_TYPE_LABEL[activeTask.type] }} - 流程办理</h2>

          <div v-if="activeTask.type === 'ORDER_URGENT'" class="step-block" v-loading="stepLoading">
            <el-steps :active="stepNo" finish-status="success" simple>
              <el-step title="第一步" />
              <el-step title="第二步" />
              <el-step title="第三步" />
              <el-step title="第四步" />
            </el-steps>

            <div class="step-card">
              <div class="step-title">第一步：输入合同编号</div>
              <el-input v-model="step1ContractNo" placeholder="请输入合同编号" />
              <el-button type="primary" style="margin-top: 10px" @click="submitStep1()">提交并继续</el-button>
            </div>

            <div v-if="step2Data && stepNo >= 2" class="step-card">
              <div class="step-title">第二步：订单数据分析结果</div>
              <div class="step-info">
                合同编号：{{ step2Data.contractNo }}
                CRM编号：{{ step2Data.extracted.crmNumber }}
                部门名称：{{ step2Data.extracted.salesDept }}
                项目名称：{{ step2Data.extracted.projectName }}
              </div>

              <el-form label-width="180px">
                <el-form-item label="项目紧急性">
                  <el-select v-model="urgentForm.projectUrgency" style="width: 220px">
                    <el-option label="一般" value="一般" />
                    <el-option label="紧急" value="紧急" />
                  </el-select>
                </el-form-item>
                <el-form-item label="最迟需要到货时间">
                  <div style="width: 100%">
                    <el-date-picker
                      v-model="urgentForm.latestArrivalTime"
                      type="date"
                      value-format="YYYY-MM-DD"
                      style="width: 220px"
                      @change="handleLatestArrivalChange"
                    />
                    <div class="hint">该字段为订单批量设置，填写之后符合加急条件的数据将批量填写</div>
                  </div>
                </el-form-item>
                <el-form-item label="是否接受分批发货">
                  <el-radio-group v-model="urgentForm.acceptPartialShipment">
                    <el-radio value="是">是</el-radio>
                    <el-radio value="否">否</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="最紧急的商品清单">
                  <el-input v-model="urgentForm.mostUrgentProductList" />
                </el-form-item>
                <el-form-item label="不及时交付的影响">
                  <el-input v-model="urgentForm.delayedDeliveryImpact" type="textarea" :rows="2" />
                </el-form-item>
              </el-form>

              <el-table :data="step2Data.productArray" border>
                <el-table-column label="选择" width="60">
                  <template #default="scope">
                    <el-checkbox v-model="scope.row.selected" :disabled="!scope.row['可勾选']" />
                  </template>
                </el-table-column>
                <el-table-column prop="市场代码名称" label="产品型号" min-width="130" />
                <el-table-column prop="物料描述" label="物料描述" min-width="150" />
                <el-table-column prop="未发货数量" label="未发货数量" width="110" />
                <el-table-column prop="客户期望交期" label="客户期望日期" width="130" />
                <el-table-column prop="约定交期-ODC" label="约定交期-ODC" width="130" />
                <el-table-column prop="交期空白原因" label="交期空白原因" min-width="220" />
                <el-table-column prop="最迟到货时间" label="最迟需要到货时间" width="140" />
              </el-table>

              <el-button type="primary" style="margin-top: 10px" @click="nextToStep3">下一步：获取计划数据</el-button>
            </div>

            <div v-if="stepNo >= 3 && step2Data" class="step-card">
              <div class="step-title">第三步：计划数据与统筹匹配</div>
              <div>区域统筹：{{ step2Data.coordinator.name }} ({{ step2Data.coordinator.feishuId }})</div>
              <div>代理区域统筹：{{ step2Data.coordinator.agentName || '-' }}</div>
              <div>盘点详情链接：{{ planData?.项目盘点详情url || '-' }}</div>
              <el-table :data="planData?.盘点记录 || []" border style="margin-top: 10px">
                <el-table-column prop="产品型号" label="产品型号" min-width="120" />
                <el-table-column prop="数量" label="数量" width="80" />
                <el-table-column prop="客期" label="客期" width="120" />
                <el-table-column prop="勾选或取消备货时间" label="勾选或取消备货时间" min-width="160" />
              </el-table>
              <el-button type="primary" style="margin-top: 10px" @click="submitFinal">提交到任务管理平台</el-button>
            </div>

            <div v-if="stepNo >= 4" class="step-card success">
              第四步已完成：数据已传输到任务管理平台，并已完成加急订单关注接口调用。
            </div>
          </div>

          <div v-else-if="activeTask.type === 'UNORDERED_ASSESS'" class="step-block" v-loading="stepLoading">
            <el-steps :active="unorderedStepNo" finish-status="success" simple>
              <el-step title="第一步" />
              <el-step title="第二步" />
              <el-step title="第三步" />
            </el-steps>

            <div class="step-card">
              <div class="step-title">第一步：填写未下单咨询信息</div>
              <el-form label-width="130px">
                <el-form-item label="CRM编号">
                  <el-input v-model="unorderedForm.crmNo" placeholder="CRM编号与产品信息二选一" />
                </el-form-item>
                <el-form-item label="产品信息">
                  <el-input
                    v-model="unorderedForm.productInfo"
                    type="textarea"
                    :rows="4"
                    placeholder="每行格式：产品型号*数量 或 产品型号 数量"
                  />
                </el-form-item>
                <el-form-item label="需求时间">
                  <el-date-picker v-model="unorderedForm.needTime" type="date" value-format="YYYY-MM-DD" />
                </el-form-item>
                <el-form-item label="备注(报备+项目情况)">
                  <el-input v-model="unorderedForm.remark" type="textarea" :rows="2" />
                </el-form-item>
              </el-form>
              <el-button type="primary" @click="submitUnorderedStep1">提交并继续</el-button>
            </div>

            <div v-if="unorderedAnalyze && unorderedStepNo >= 2" class="step-card">
              <div class="step-title">第二步：确认提交流水</div>
              <div class="step-info">
                提交模式：{{ unorderedAnalyze.judge === 'CRM' ? 'CRM编号分支' : '产品型号分支' }}
                需求时间：{{ unorderedAnalyze.needTime }}
              </div>
              <el-table :data="unorderedPlanRows" border>
                <el-table-column label="选择" width="60">
                  <template #default="scope">
                    <el-checkbox v-model="scope.row.selected" />
                  </template>
                </el-table-column>
                <el-table-column prop="CRM编号" label="CRM编号" min-width="140" />
                <el-table-column prop="产品型号" label="产品型号" min-width="150" />
                <el-table-column prop="产品名称" label="产品名称" min-width="140" />
                <el-table-column prop="数量" label="数量" width="80" />
                <el-table-column prop="要求发货日期" label="要求发货日期" min-width="120" />
                <el-table-column prop="是否备货" label="是否备货" width="90" />
                <el-table-column prop="产品统筹" label="产品统筹" min-width="120" />
              </el-table>
              <el-button type="primary" style="margin-top: 10px" @click="nextUnorderedStep">下一步：重复校验并提交</el-button>
            </div>

            <div v-if="unorderedStepNo >= 3" class="step-card success">
              第三步已完成：数据已传输到任务管理平台“未下单交期评估任务”。
            </div>

            <el-dialog v-model="todayDuplicateVisible" width="860px" title="检测到当天已评估任务">
              <el-table :data="todayDuplicateRows" border>
                <el-table-column prop="CRM编号" label="CRM编号" min-width="140" />
                <el-table-column prop="产品型号" label="产品型号" min-width="130" />
                <el-table-column prop="数量" label="数量" width="80" />
                <el-table-column prop="客户期望日期" label="客户期望日期" min-width="120" />
                <el-table-column prop="需求日期" label="需求日期" min-width="120" />
                <el-table-column prop="备注" label="备注" min-width="160" />
                <el-table-column prop="评估预计交期" label="评估预计交期" min-width="140" />
              </el-table>
              <template #footer>
                <el-button @click="todayDuplicateVisible = false">取消</el-button>
                <el-button @click="continueTodayDuplicate">单独提交任务</el-button>
                <el-button type="primary" @click="joinTodayDuplicate">加入已评估任务</el-button>
              </template>
            </el-dialog>

            <el-dialog v-model="historyDuplicateVisible" width="560px" title="历史重复提交提醒">
              <p>您输入的CRM编号存在已提交的记录，请仔细核对后再提交！</p>
              <template #footer>
                <el-button @click="historyDuplicateVisible = false">取消</el-button>
                <el-button @click="submitAgainNormal">再次提交(普通项目)</el-button>
                <el-button type="danger" @click="submitAgainImportant">再次提交(重要项目)</el-button>
              </template>
            </el-dialog>
          </div>

          <div v-else-if="activeTask.type === 'DELIVERY_CHANGE'" class="step-block" v-loading="stepLoading">
            <el-steps :active="deliveryStepNo" finish-status="success" simple>
              <el-step title="第一步" />
              <el-step title="第二步" />
              <el-step title="第三步" />
            </el-steps>

            <div class="step-card">
              <div class="step-title">第一步：输入合同编号和变更类型</div>
              <el-form label-width="120px">
                <el-form-item label="合同编号"><el-input v-model="deliveryForm.contractNo" placeholder="请输入合同编号" /></el-form-item>
                <el-form-item label="变更类型">
                  <el-radio-group v-model="deliveryForm.taskType">
                    <el-radio value="客期提前">客期提前</el-radio>
                    <el-radio value="客期延后">客期延后</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-form>
              <el-button type="primary" @click="submitDeliveryStep1">提交并继续</el-button>
            </div>

            <div v-if="deliveryAnalyze && deliveryStepNo >= 2" class="step-card">
              <div class="step-title">第二步：核对订单数据并设置客期变更字段</div>
              <div class="step-info">
                合同编号：{{ deliveryAnalyze.contractNo }}
                CRM编号：{{ deliveryAnalyze.extracted.crmNumber }}
                部门名称：{{ deliveryAnalyze.extracted.salesDept }}
                项目名称：{{ deliveryAnalyze.extracted.projectName }}
              </div>

              <el-form label-width="180px">
                <el-form-item :label="deliveryForm.taskType === '客期提前' ? '客期提前至' : '客期延后至'">
                  <div style="width: 100%">
                    <el-date-picker
                      v-model="deliveryForm.batchTargetDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      @change="applyDeliveryBatchDate"
                    />
                    <div class="hint">该字段为客期批量设置，填写之后符合客期变更条件的数据将批量填写</div>
                  </div>
                </el-form-item>

                <el-form-item v-if="deliveryForm.taskType === '客期提前'" label="提前原因">
                  <el-input v-model="deliveryForm.changeReason" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item v-if="deliveryForm.taskType === '客期提前'" label="若不齐套是否同意分批发货">
                  <el-radio-group v-model="deliveryForm.allowPartialShipment">
                    <el-radio value="同意">同意</el-radio>
                    <el-radio value="不同意">不同意</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item v-if="deliveryForm.taskType === '客期延后'" label="延后原因">
                  <el-input v-model="deliveryForm.delayReason" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item v-if="deliveryForm.taskType === '客期延后'" label="延后证明">
                  <el-upload :auto-upload="false" :limit="1" :on-change="handleDelayProofChange">
                    <el-button>上传附件</el-button>
                    <template #tip><span style="margin-left: 8px; color: #64748b">{{ deliveryForm.delayProof || '未上传' }}</span></template>
                  </el-upload>
                </el-form-item>
              </el-form>

              <el-table :data="deliveryAnalyze.productArray" border>
                <el-table-column label="选择" width="60">
                  <template #default="scope">
                    <el-checkbox v-if="scope.row['可勾选']" v-model="scope.row.selected" />
                  </template>
                </el-table-column>
                <el-table-column prop="市场代码名称" label="市场代码名称" min-width="130" />
                <el-table-column prop="物料描述" label="物料描述" min-width="170" />
                <el-table-column prop="未发货数量" label="未发货数量" width="110" />
                <el-table-column prop="客户期望交期" label="客户期望日期" width="130" />
                <el-table-column prop="约定交期-ODC" label="约定交期-ODC" width="130" />
                <el-table-column label="变更数量" width="110">
                  <template #default="scope">
                    <el-input-number v-model="scope.row['变更数量']" :min="1" :step="1" step-strictly />
                  </template>
                </el-table-column>
                <el-table-column :label="deliveryForm.taskType === '客期提前' ? '客期提前至' : '客期延后至'" width="150">
                  <template #default="scope">
                    <el-date-picker
                      v-if="scope.row['可勾选']"
                      v-model="scope.row[deliveryForm.taskType === '客期提前' ? '客期提前至' : '客期延后至']"
                      type="date"
                      value-format="YYYY-MM-DD"
                    />
                  </template>
                </el-table-column>
              </el-table>

              <el-button type="primary" style="margin-top: 10px" @click="nextDeliveryStep">下一步：获取计划数据</el-button>
            </div>

            <div v-if="deliveryStepNo >= 3 && deliveryAnalyze" class="step-card">
              <div class="step-title">第三步：确认计划数据并提交任务管理平台</div>
              <div>区域统筹：{{ deliveryAnalyze.extracted.salesDept }} / {{ deliveryAnalyze.extracted.region }}</div>
              <div>盘点详情链接：{{ deliveryPlan?.项目盘点详情url || '-' }}</div>
              <el-table :data="deliveryPlan?.盘点记录 || []" border style="margin-top: 10px">
                <el-table-column prop="产品型号" label="产品型号" min-width="120" />
                <el-table-column prop="数量" label="数量" width="80" />
                <el-table-column prop="客期" label="客期" width="120" />
              </el-table>
              <el-button type="primary" style="margin-top: 10px" @click="submitDeliveryFinal">提交到任务管理平台</el-button>
            </div>
          </div>

          <div v-else class="step-block">
            <div class="step-grid">
              <div v-for="(value, key) in activeTask.payload" :key="key" class="step-item">
                <span class="label">{{ key }}</span>
                <span class="value">{{ value || '-' }}</span>
              </div>
            </div>
            <p class="raw-input"><strong>原始输入：</strong>{{ activeTask.rawInput || '手动开启，待填写第一步信息' }}</p>
          </div>
        </template>
      </article>

        <div class="action-bar">
          <el-button round @click="consultHuman">人工咨询</el-button>
          <el-button round type="danger" @click="goManagement">任务管理平台</el-button>
        </div>

        <div class="input-wrap">
          <el-input
            v-model="aiInput"
            type="textarea"
            :rows="4"
            placeholder="请输入任务描述，或先在左侧选择任务类型"
          />
          <div class="input-footer">
            <el-button type="primary" :loading="parsing" @click="submitAiInput">智能解析</el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.user-shell {
  position: fixed;
  inset: 0;
  z-index: 50;
  min-height: 100vh;
  overflow: auto;
  background:
    radial-gradient(1200px 500px at 85% -100px, rgba(40, 90, 204, 0.2), transparent 60%),
    linear-gradient(180deg, #eef2f8, #f6f8fb 38%, #f1f4f9);
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
}

.user-topbar {
  height: 68px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #dbe3f0;
  backdrop-filter: saturate(130%) blur(4px);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 34px;
  height: 28px;
  border-radius: 6px;
  background: linear-gradient(145deg, #365fc9, #29489f);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  box-shadow: 0 6px 14px rgba(42, 79, 175, 0.32);
}

.brand-name {
  font-size: 24px;
  font-weight: 700;
  color: #1d2a43;
}

.user-workbench {
  display: grid;
  grid-template-columns: 290px 1fr;
  min-height: calc(100vh - 68px);
}

.left-panel {
  padding: 16px 12px;
  border-right: 1px solid #d8e0ec;
  background: linear-gradient(180deg, #f8fbff, #edf3fb 35%, #e9eef7);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.8);
}

.new-task-btn {
  width: 100%;
  margin-bottom: 14px;
  height: 38px;
}

.manual-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}

.manual-item {
  border: 1px solid #d9e2f1;
  background: rgba(255, 255, 255, 0.8);
  text-align: left;
  padding: 10px 10px;
  cursor: pointer;
  color: #203050;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.manual-item:hover {
  transform: translateY(-1px);
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(31, 61, 128, 0.1);
}

.manual-item.active {
  border-color: #8aa7e8;
  background: linear-gradient(90deg, #e2ecff, #d5e3ff);
  color: #274cae;
  font-weight: 600;
}

.history-title {
  margin: 10px 2px;
  font-size: 13px;
  font-weight: 700;
  color: #4769b8;
  letter-spacing: 0.04em;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: calc(100vh - 385px);
  overflow: auto;
}

.history-item {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #dce4f0;
  background: rgba(255, 255, 255, 0.74);
}

.history-item:hover {
  background: #ffffff;
  border-color: #cbd7ea;
}

.history-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #394254;
}

.history-date {
  color: #6e7e99;
  font-size: 12px;
}

.main-panel {
  padding: 16px 18px;
}

.profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #d2dced;
  font-size: 12px;
  color: #3e4f72;
  background: linear-gradient(145deg, #f7f9fd, #ecf1fb);
}

.name {
  font-weight: 600;
  color: #2c3d61;
}

.chat-card {
  min-height: 430px;
  padding: 20px 22px;
  border: 1px solid #dbe5f2;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #fbfdff 70%, #f8fbff);
  box-shadow: 0 16px 36px rgba(25, 54, 112, 0.1);
}

.chat-card :deep(.el-steps) {
  background: #f7f9ff;
  border-radius: 10px;
  padding: 8px;
}

.chat-card :deep(.el-step__title) {
  font-size: 13px;
}

.chat-card h2 {
  margin: 0 0 16px;
  font-size: 26px;
  font-weight: 700;
  color: #1d2b46;
}

.welcome-line {
  margin: 8px 0;
  line-height: 1.75;
  color: #314258;
  white-space: pre-wrap;
}

.step-block {
  background: #f6faff;
  border: 1px solid #d9e5f7;
  border-radius: 12px;
  padding: 14px;
}

.step-block :deep(.el-loading-mask) {
  background: rgba(255, 255, 255, 0.35);
}

.step-card {
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #dfe7f4;
}

.step-title {
  margin-bottom: 8px;
  font-weight: 700;
  color: #1f2937;
}

.step-info {
  line-height: 1.8;
  color: #2d4059;
  white-space: pre-line;
}

.hint {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.success {
  border-color: #7fdba8;
  background: #f0fff6;
  color: #065f46;
}

.step-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 10px;
}

.step-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border-radius: 8px;
  background: #fff;
}

.label {
  font-size: 12px;
  color: #8a93a3;
}

.value {
  color: #222b3d;
  word-break: break-all;
}

.raw-input {
  margin: 12px 0 0;
  color: #334155;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  margin: 14px 2px 10px;
}

.input-wrap {
  border: 1px solid #d5dfef;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  padding: 12px 14px;
  box-shadow: 0 10px 22px rgba(37, 62, 112, 0.06);
}

.input-wrap :deep(textarea.el-textarea__inner) {
  border-radius: 12px;
  min-height: 96px !important;
  font-size: 15px;
}

.input-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .user-topbar {
    height: auto;
    gap: 8px;
    padding: 12px;
  }

  .brand-name {
    font-size: 20px;
  }

  .user-workbench {
    grid-template-columns: 1fr;
  }

  .left-panel {
    border-right: 0;
    border-bottom: 1px solid #e4e7ec;
  }

  .chat-card h2 {
    font-size: 22px;
  }

  .step-grid {
    grid-template-columns: 1fr;
  }
}
</style>
