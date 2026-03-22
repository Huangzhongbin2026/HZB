import kimiConfig from '@/config/ai/kimi-config.json'
import prompts from '@/config/ai/task-prompts.json'

export type UserTaskType = 'ORDER_URGENT' | 'UNORDERED_ASSESS' | 'DELIVERY_CHANGE'

export interface AiParseResult {
  taskType: UserTaskType
  payload: Record<string, string>
  rawClassify: string
}

interface KimiResponse {
  choices?: Array<{ message?: { content?: string } }>
}

const TASK_TYPE_NAME_MAP: Array<{ key: UserTaskType; aliases: string[] }> = [
  { key: 'UNORDERED_ASSESS', aliases: ['未下单交期咨询', '未下单咨询', '未下单交期评估'] },
  { key: 'ORDER_URGENT', aliases: ['订单交期查询 / 加急', '订单交期查询/加急', '订单加急', '交期查询', '加急'] },
  { key: 'DELIVERY_CHANGE', aliases: ['已下订单客户期望日期变更', '客期变更', '客户期望日期变更'] },
]

const readContent = (data: KimiResponse) => data.choices?.[0]?.message?.content?.trim() || ''

const callKimi = async (systemPrompt: string, userInput: string) => {
  const response = await fetch(kimiConfig.baseUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${kimiConfig.apiKey}`,
    },
    body: JSON.stringify({
      model: kimiConfig.model,
      temperature: kimiConfig.temperature,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userInput },
      ],
    }),
  })

  if (!response.ok) {
    throw new Error(`Kimi request failed: ${response.status}`)
  }

  const data = (await response.json()) as KimiResponse
  return readContent(data)
}

const mapTaskType = (content: string): UserTaskType | null => {
  const normalized = content.replace(/\s/g, '')
  const hit = TASK_TYPE_NAME_MAP.find((item) => item.aliases.some((alias) => normalized.includes(alias.replace(/\s/g, ''))))
  return hit?.key || null
}

const extractJson = (rawText: string) => {
  const match = rawText.match(/\{[\s\S]*\}/)
  if (!match) {
    throw new Error('AI response has no JSON body')
  }
  return JSON.parse(match[0]) as Record<string, unknown>
}

const normalizePayload = (payload: Record<string, unknown>) => {
  const normalized: Record<string, string> = {}
  Object.keys(payload).forEach((key) => {
    const value = payload[key]
    normalized[key] = value == null ? '' : String(value)
  })
  return normalized
}

const fallbackParse = (input: string): AiParseResult | null => {
  const text = input.trim()
  const contractNo = (text.match(/合同(?:编号)?[:：\s]*([A-Za-z0-9-]{6,})/) || [])[1] || ''
  const crmNo = (text.match(/CRM(?:编号)?[:：\s]*([A-Za-z0-9-]{6,})/i) || [])[1] || ''
  const needDate = (text.match(/(20\d{2})[年\-./]?(\d{1,2})[月\-./]?(\d{1,2})/) || [])
  const normalizedDate =
    needDate.length === 4
      ? `${needDate[1]}-${needDate[2].padStart(2, '0')}-${needDate[3].padStart(2, '0')}`
      : ''

  if (crmNo) {
    return {
      taskType: 'UNORDERED_ASSESS',
      payload: {
        crmNo,
        productInformation: '',
        needTime: normalizedDate,
        remarks: text,
        changeScope: '全部',
      },
      rawClassify: '匹配任务类型：未下单交期咨询',
    }
  }

  if (contractNo && /(变更|提前|延后|客期)/.test(text)) {
    return {
      taskType: 'DELIVERY_CHANGE',
      payload: {
        questionType: /(延后)/.test(text) ? '客期延后' : '客期提前',
        contractNo,
        changeReason: text,
        targetDeliveryDate: normalizedDate,
        allowPartialShipmentIfIncomplete: /(不能分开|不允许分批)/.test(text)
          ? '否'
          : /(可以分批|允许分批)/.test(text)
            ? '是'
            : '',
      },
      rawClassify: '匹配任务类型：已下订单客户期望日期变更',
    }
  }

  if (contractNo) {
    return {
      taskType: 'ORDER_URGENT',
      payload: {
        contractNo,
        projectUrgency: /(加急|催单|紧急)/.test(text) ? '紧急' : '一般',
        latestArrivalTime: normalizedDate,
        acceptPartialShipment: /(可以分批|允许分批)/.test(text) ? '是' : '否',
        mostUrgentProductList: '',
        delayedDeliveryImpact: text,
        changeScope: '全部提交',
      },
      rawClassify: '匹配任务类型：订单交期查询 / 加急',
    }
  }

  return null
}

export const smartParseTask = async (input: string): Promise<AiParseResult | null> => {
  const text = input.trim()
  if (!text) {
    return null
  }

  if (!kimiConfig.enabled || !kimiConfig.apiKey) {
    return fallbackParse(text)
  }

  const classifyText = await callKimi(prompts.classifyPrompt, text)
  if (!classifyText || classifyText.includes('无法判定任务类型')) {
    return null
  }

  const taskType = mapTaskType(classifyText)
  if (!taskType) {
    return null
  }

  const extractionPrompt = prompts.extractPrompts[taskType]
  const extractionText = await callKimi(extractionPrompt, text)
  if (!extractionText || extractionText.includes('解析失败')) {
    return null
  }

  const payload = normalizePayload(extractJson(extractionText))
  return { taskType, payload, rawClassify: classifyText }
}
