<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

type ViewName = 'home' | 'notes' | 'graph' | 'ai' | 'settings'
type SaveState = 'idle' | 'dirty' | 'saving' | 'saved' | 'error'
type SyncEventState = 'pending' | 'applying' | 'acked' | 'error'
type DirectoryAction = 'create-folder' | 'rename-folder' | 'move-note' | null
type AiAction = 'summary' | 'expand' | 'polish' | 'qa' | 'create'
type RightTab = 'outline' | 'links' | 'tags' | 'properties' | 'ai'

type SyncEventRecord = {
  id: number
  note_slug: string
  event_type: string
  path: string
  target_path: string
  status: SyncEventState
  created_at: string
  message: string
}

type NoteRecord = {
  id: number
  title: string
  slug: string
  content: string
  path: string
  parent_path: string
  tags: string[]
  links: string[]
  summary: string
  version: string
  updated_at: string
}

type LinkRecord = {
  source_slug: string
  target_slug: string
  target_title: string
  is_resolved: boolean
}

type SearchResult = {
  note_slug: string
  title: string
  path: string
  snippet: string
}

type TagSummary = {
  tag: string
  count: number
}

type SyncStatus = {
  pending_events: number
  connected_desktop_devices: number
  last_event_id: number | null
}

type ClipPreviewResult = {
  note?: NoteRecord | null
  extracted_title: string
  source_url: string
  summary: string
  preview_content: string
  saved: boolean
}

type UploadedDocument = {
  filename: string
  extracted_title: string
  content_preview: string
  summary: string
  saved_note?: NoteRecord | null
}

type AiReference = {
  title: string
  note_slug?: string
  snippet: string
  score: string
}

type AiProviderPreset = {
  provider_id: string
  label: string
  base_url: string
  models: string[]
}

type AiProviderConfig = {
  provider_id: string
  provider_label: string
  base_url: string
  model_name: string
  api_key_masked: string
  has_api_key: boolean
  is_enabled: boolean
}

type CurrentUser = {
  id: number
  username: string
  display_name: string
  role: string
  instance_id: string
  edition: 'personal' | 'team'
  default_space_slug: string
}

type InstanceConfig = {
  instance_id: string
  instance_name: string
  deployment_mode: 'desktop' | 'server'
  edition: 'personal' | 'team'
  desktop_host_enabled: boolean
  server_host_enabled: boolean
  auth_required: boolean
  team_license_verified: boolean
  is_initialized: boolean
}

const apiBase = import.meta.env.VITE_API_BASE || 'http://127.0.0.1:8000/api/v1'
const deviceId = 'desktop-windows'
const tokenStorageKey = 'knowledge-cloud-desktop-token'
const rootStorageKey = 'knowledge-cloud-desktop-root'
const autoSyncStorageKey = 'knowledge-cloud-desktop-auto-sync'
const recentVisitedStorageKey = 'knowledge-cloud-desktop-recent-visited'

const selectedView = ref<ViewName>('home')
const syncSocket = ref<WebSocket | null>(null)
const username = ref('admin')
const password = ref('admin123456')
const token = ref(localStorage.getItem(tokenStorageKey) ?? '')
const currentUser = ref<CurrentUser | null>(null)
const instanceConfig = ref<InstanceConfig | null>(null)
const instanceBootstrapForm = ref({
  instance_name: '个人双向知识库',
  deployment_mode: 'desktop' as 'desktop' | 'server',
  edition: 'personal' as 'personal' | 'team',
  authorization_code: '',
})
const instanceBootstrapStatus = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')
const desktopRootPath = ref(localStorage.getItem(rootStorageKey) ?? 'D:/KnowledgeCloudVault')
const autoSyncEnabled = ref(localStorage.getItem(autoSyncStorageKey) !== 'false')
const syncStatus = ref<SyncStatus>({ pending_events: 0, connected_desktop_devices: 0, last_event_id: null })
const events = ref<SyncEventRecord[]>([])
const notes = ref<NoteRecord[]>([])
const currentNote = ref<NoteRecord | null>(null)
const backlinks = ref<{ note_slug: string; incoming: LinkRecord[]; outgoing: LinkRecord[] }>({
  note_slug: '',
  incoming: [],
  outgoing: [],
})
const tags = ref<TagSummary[]>([])
const searchQuery = ref('')
const searchResults = ref<SearchResult[]>([])
const activeTagFilter = ref('')
const recentVisited = ref<string[]>(JSON.parse(localStorage.getItem(recentVisitedStorageKey) ?? '[]'))
const saveState = ref<SaveState>('idle')
const isBusy = ref(false)
const errorMessage = ref('')
const agentMessage = ref('未校验本地副本目录')
const lastSyncedAt = ref('')
const isApplyingEvents = ref(false)
const clipUrl = ref('')
const clipSummarizeWithAi = ref(true)
const clipSummaryPrompt = ref('请提炼网页的关键信息、结构脉络和可执行建议。')
const clipPreview = ref<ClipPreviewResult | null>(null)
const showClipPreview = ref(false)
const aiPrompt = ref('')
const aiResponse = ref('')
const aiReferences = ref<AiReference[]>([])
const showAiPreview = ref(false)
const uploadedDocument = ref<UploadedDocument | null>(null)
const useRemoteAi = ref(true)
const useRag = ref(true)
const providerPresets = ref<AiProviderPreset[]>([])
const providerCurrent = ref<AiProviderConfig | null>(null)
const providerForm = ref({
  provider_id: 'custom',
  provider_label: '自定义 OpenAI 兼容',
  base_url: 'https://api.openai.com/v1',
  model_name: 'gpt-5',
  api_key: '',
  is_enabled: true,
})
const directoryAction = ref<DirectoryAction>(null)
const folderPath = ref('')
const folderSourcePath = ref('')
const folderTargetPath = ref('')
const noteTargetPath = ref('')
const graphZoom = ref(1)
const rightTab = ref<RightTab>('outline')
const markdownEditor = ref<HTMLTextAreaElement | null>(null)
const explorerCollapsed = ref(false)
const inspectorCollapsed = ref(false)
const explorerPaneWidth = ref(296)
const inspectorPaneWidth = ref(284)
const showLoginModal = ref(false)
const loginModalReason = ref<'initial' | 'manual' | 'expired'>('initial')

let pollTimer: number | undefined
let autosaveTimer: number | undefined
let reprocessRequested = false

const navItems = computed(() => [
  { id: 'home' as const, label: '首页', caption: '搜索与概览' },
  { id: 'notes' as const, label: '笔记', caption: '目录与编辑' },
  { id: 'graph' as const, label: '图谱', caption: '关系网络' },
  { id: 'ai' as const, label: '创作', caption: 'AI 与资料提炼' },
  { id: 'settings' as const, label: '系统设置', caption: '桌面同步与模型' },
])

const isConnected = computed(() => Boolean(token.value))
const isInstanceAdmin = computed(() => currentUser.value?.role === 'admin')
const hasTauriRuntime = computed(() => Boolean((window as Window & { __TAURI_INTERNALS__?: unknown }).__TAURI_INTERNALS__))
const pendingLocalEvents = computed(() => events.value.filter((event) => event.status === 'pending').length)
const visibleTags = computed(() => tags.value.slice(0, 12))
const filteredNotes = computed(() => {
  const source = activeTagFilter.value ? notes.value.filter((note) => note.tags.includes(activeTagFilter.value)) : notes.value
  return [...source].sort((left, right) => right.updated_at.localeCompare(left.updated_at))
})
const groupedNotes = computed(() => {
  const groups = new Map<string, NoteRecord[]>()
  for (const note of filteredNotes.value) {
    const folder = note.parent_path || '未分类'
    const current = groups.get(folder) ?? []
    current.push(note)
    groups.set(folder, current)
  }
  return [...groups.entries()].map(([folder, grouped]) => ({ folder, notes: grouped }))
})
const recentVisitedNotes = computed(() => {
  const noteMap = new Map(notes.value.map((note) => [note.slug, note]))
  return recentVisited.value.map((slug) => noteMap.get(slug)).filter(Boolean) as NoteRecord[]
})
const homeRecentNotes = computed(() => (recentVisitedNotes.value.length > 0 ? recentVisitedNotes.value.slice(0, 4) : filteredNotes.value.slice(0, 4)))
const currentFolderPath = computed(() => {
  const path = currentNote.value?.path ?? ''
  const segments = path.split('/').filter(Boolean)
  segments.pop()
  return segments.join('/')
})
const currentPresetModels = computed(
  () => providerPresets.value.find((item) => item.provider_id === providerForm.value.provider_id)?.models ?? [],
)
const noteOutline = computed(() => {
  if (!currentNote.value) {
    return [] as Array<{ level: number; text: string; line: number }>
  }

  return currentNote.value.content
    .split('\n')
    .map((line, index) => {
      const matched = /^(#{1,6})\s+(.+)$/.exec(line)
      if (!matched) {
        return null
      }
      return {
        level: matched[1].length,
        text: matched[2],
        line: index,
      }
    })
    .filter(Boolean) as Array<{ level: number; text: string; line: number }>
})
const noteWordCount = computed(() => {
  if (!currentNote.value?.content) {
    return 0
  }
  return currentNote.value.content.trim().split(/\s+/).filter(Boolean).length
})
const noteReadingMinutes = computed(() => Math.max(1, Math.ceil(noteWordCount.value / 220)))
const noteCharacterCount = computed(() => currentNote.value?.content.length ?? 0)
const saveLabel = computed(() => {
  if (saveState.value === 'saving') {
    return '自动保存中'
  }
  if (saveState.value === 'saved') {
    return '已同步保存'
  }
  if (saveState.value === 'error') {
    return '保存失败'
  }
  if (saveState.value === 'dirty') {
    return '有未保存修改'
  }
  return `${syncStatus.value.pending_events} 待同步`
})
const graphNodes = computed(() => {
  const ordered = currentNote.value
    ? [currentNote.value, ...notes.value.filter((note) => note.slug !== currentNote.value?.slug)]
    : notes.value
  const visible = ordered.slice(0, 14)
  const palette = ['#1f5eff', '#f06b47', '#0f8a70', '#7b61ff', '#d69b00', '#c44f7a']

  return visible.map((note, index) => {
    const angle = (Math.PI * 2 * index) / Math.max(visible.length, 1)
    const isFocused = note.slug === currentNote.value?.slug
    const radius = isFocused ? 0 : 34 + (index % 3) * 9
    const x = isFocused ? 50 : 50 + Math.cos(angle) * radius
    const y = isFocused ? 50 : 50 + Math.sin(angle) * (radius * 0.72)
    const degree = visible.filter((item) => item.links.includes(note.slug) || note.links.includes(item.slug)).length

    return {
      ...note,
      x,
      y,
      size: 14 + Math.min(degree, 6) * 2 + note.tags.length,
      degree,
      color: palette[index % palette.length],
      isFocused,
    }
  })
})
const graphEdges = computed(() => {
  const nodeMap = new Map(graphNodes.value.map((node) => [node.slug, node]))
  const edgeKeys = new Set<string>()
  const edges: Array<{ key: string; x1: number; y1: number; x2: number; y2: number; emphasis: boolean }> = []

  for (const node of graphNodes.value) {
    for (const targetSlug of node.links) {
      const target = nodeMap.get(targetSlug)
      if (!target) {
        continue
      }
      const key = [node.slug, target.slug].sort().join('::')
      if (edgeKeys.has(key)) {
        continue
      }
      edgeKeys.add(key)
      edges.push({
        key,
        x1: node.x,
        y1: node.y,
        x2: target.x,
        y2: target.y,
        emphasis: node.slug === currentNote.value?.slug || target.slug === currentNote.value?.slug,
      })
    }
  }

  return edges
})
const denseNotes = computed(() => [...graphNodes.value].sort((left, right) => right.degree - left.degree).slice(0, 5))
const graphStats = computed(() => ({
  nodes: notes.value.length,
  edges: notes.value.reduce((total, note) => total + note.links.length, 0),
  tags: tags.value.length,
}))
const graphTransform = computed(() => `translate(50 50) scale(${graphZoom.value.toFixed(2)}) translate(-50 -50)`)
const notesLayoutStyle = computed(() => ({
  gridTemplateColumns: `${explorerCollapsed.value ? '56px' : `${explorerPaneWidth.value}px`} minmax(0, 1fr) ${inspectorCollapsed.value ? '56px' : `${inspectorPaneWidth.value}px`}`,
}))
const displayUserName = computed(() => currentUser.value?.display_name || currentUser.value?.username || '未登录')
const displayUserMeta = computed(() => {
  if (!currentUser.value) {
    return '点击登录'
  }
  return `@${currentUser.value.username} · ${currentUser.value.role === 'admin' ? '管理员' : '成员'}`
})
const displayUserInitial = computed(() => displayUserName.value.slice(0, 1).toUpperCase())
const loginModalTitle = computed(() => (loginModalReason.value === 'expired' ? '登录已失效' : '登录知识库'))
const loginModalDescription = computed(() => {
  if (loginModalReason.value === 'expired') {
    return '当前登录状态已过期，请重新验证账户。'
  }
  if (currentUser.value) {
    return '当前桌面端已记住登录状态，需要时可以切换账户。'
  }
  return '首次进入请先登录，后续会自动保留当前登录状态。'
})
const homeMetrics = computed(() => [
  { label: '笔记', value: notes.value.length },
  { label: '标签', value: tags.value.length },
  { label: '设备', value: syncStatus.value.connected_desktop_devices },
  { label: '待同步', value: syncStatus.value.pending_events },
])

watch(desktopRootPath, (value) => {
  localStorage.setItem(rootStorageKey, value)
})

watch(autoSyncEnabled, (value) => {
  localStorage.setItem(autoSyncStorageKey, String(value))
  if (value) {
    void processPendingEvents()
  }
})

function persistRecentVisited() {
  localStorage.setItem(recentVisitedStorageKey, JSON.stringify(recentVisited.value.slice(0, 8)))
}

function applyToken(nextToken: string) {
  token.value = nextToken
  localStorage.setItem(tokenStorageKey, nextToken)
}

function clearToken() {
  token.value = ''
  localStorage.removeItem(tokenStorageKey)
  syncSocket.value?.close()
  syncSocket.value = null
}

function parseErrorMessage(payload: unknown, fallback: string) {
  if (!payload || typeof payload !== 'object') {
    return fallback
  }
  const detail = (payload as { detail?: unknown }).detail
  if (typeof detail === 'string') {
    return detail
  }
  if (detail && typeof detail === 'object') {
    const message = (detail as { message?: unknown }).message
    if (typeof message === 'string') {
      return message
    }
  }
  return fallback
}

async function parseJsonSafe<T>(response: Response): Promise<T | null> {
  const text = await response.text()
  if (!text) {
    return null
  }
  return JSON.parse(text) as T
}

async function authorizedFetch(path: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers ?? {})
  if (token.value) {
    headers.set('Authorization', `Bearer ${token.value}`)
  }
  return fetch(`${apiBase}${path}`, {
    ...init,
    headers,
  })
}

async function requestJson<T>(path: string, init: RequestInit = {}, fallback = '请求失败') {
  const response = await authorizedFetch(path, init)
  const payload = await parseJsonSafe<T | { detail?: unknown }>(response)
  if (!response.ok) {
    throw new Error(parseErrorMessage(payload, fallback))
  }
  return payload as T
}

function openView(view: ViewName) {
  selectedView.value = view
}

function openLoginModal(reason: 'initial' | 'manual' | 'expired' = 'manual') {
  loginModalReason.value = reason
  errorMessage.value = ''
  showLoginModal.value = true
}

function closeLoginModal() {
  if (isConnected.value) {
    showLoginModal.value = false
  }
}

async function bootstrap() {
  try {
    instanceConfig.value = await requestJson<InstanceConfig>('/instance/config', {}, '获取实例配置失败')
    instanceBootstrapForm.value.instance_name = instanceConfig.value.instance_name
    instanceBootstrapForm.value.deployment_mode = instanceConfig.value.deployment_mode
    instanceBootstrapForm.value.edition = instanceConfig.value.edition
  } catch {
    // 保持默认配置
  }
  if (!token.value) {
    openLoginModal('initial')
    return
  }
  try {
    currentUser.value = await requestJson<CurrentUser>('/auth/me', {}, '登录状态已失效')
    connectSyncSocket()
    await loadWorkspace()
  } catch {
    clearToken()
    currentUser.value = null
    openLoginModal('expired')
  }
}

async function login() {
  errorMessage.value = ''
  isBusy.value = true
  try {
    const payload = await requestJson<{ access_token: string }>('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value }),
    }, '登录失败')
    applyToken(payload.access_token)
    currentUser.value = await requestJson<CurrentUser>('/auth/me', {}, '获取当前用户失败')
    connectSyncSocket()
    await loadWorkspace()
    showLoginModal.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    isBusy.value = false
  }
}

async function refreshStatus() {
  if (!token.value) {
    return
  }
  try {
    const payload = await requestJson<SyncStatus>('/sync/status', {}, '获取同步状态失败')
    syncStatus.value = payload
  } catch {
    // 保持上一次状态
  }
}

async function loadWorkspace() {
  if (!token.value) {
    return
  }
  isBusy.value = true
  errorMessage.value = ''
  try {
    const [notesPayload, tagsPayload, syncPayload, instancePayload] = await Promise.all([
      requestJson<NoteRecord[]>('/notes', {}, '获取笔记列表失败'),
      requestJson<TagSummary[]>('/tags', {}, '获取标签失败'),
      requestJson<SyncStatus>('/sync/status', {}, '获取同步状态失败'),
      requestJson<InstanceConfig>('/instance/config', {}, '获取实例配置失败'),
    ])
    notes.value = notesPayload
    tags.value = tagsPayload
    syncStatus.value = syncPayload
    instanceConfig.value = instancePayload
    instanceBootstrapForm.value.instance_name = instancePayload.instance_name
    instanceBootstrapForm.value.deployment_mode = instancePayload.deployment_mode
    instanceBootstrapForm.value.edition = instancePayload.edition

    if (currentNote.value) {
      const stillExists = notes.value.find((note) => note.slug === currentNote.value?.slug)
      if (stillExists) {
        await selectNote(stillExists.slug)
      } else if (notes.value[0]) {
        await selectNote(notes.value[0].slug)
      } else {
        currentNote.value = null
      }
    } else if (notes.value[0]) {
      await selectNote(notes.value[0].slug)
    }

    await loadAiProviderCatalog()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载工作区失败'
  } finally {
    isBusy.value = false
  }
}

async function refreshCollections(slug?: string) {
  const [notesPayload, tagsPayload, syncPayload] = await Promise.all([
    requestJson<NoteRecord[]>('/notes', {}, '获取笔记列表失败'),
    requestJson<TagSummary[]>('/tags', {}, '获取标签失败'),
    requestJson<SyncStatus>('/sync/status', {}, '获取同步状态失败'),
  ])
  notes.value = notesPayload
  tags.value = tagsPayload
  syncStatus.value = syncPayload

  if (slug) {
    await selectNote(slug)
    return
  }

  if (currentNote.value) {
    const matched = notes.value.find((item) => item.slug === currentNote.value?.slug)
    if (matched) {
      await selectNote(matched.slug)
    }
  }
}

async function saveInstanceConfig() {
  instanceBootstrapStatus.value = 'saving'
  errorMessage.value = ''
  try {
    const payload = await requestJson<InstanceConfig>('/instance/bootstrap', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(instanceBootstrapForm.value),
    }, '实例配置保存失败')
    instanceConfig.value = payload
    instanceBootstrapForm.value.instance_name = payload.instance_name
    instanceBootstrapForm.value.deployment_mode = payload.deployment_mode
    instanceBootstrapForm.value.edition = payload.edition
    instanceBootstrapStatus.value = 'saved'
  } catch (error) {
    instanceBootstrapStatus.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '实例配置保存失败'
  }
}

async function selectNote(slug: string) {
  if (!slug) {
    return
  }
  const [notePayload, backlinksPayload] = await Promise.all([
    requestJson<NoteRecord>(`/notes/${slug}`, {}, `获取笔记 ${slug} 失败`),
    requestJson<{ note_slug: string; incoming: LinkRecord[]; outgoing: LinkRecord[] }>(`/links/${slug}`, {}, '获取链接关系失败'),
  ])
  currentNote.value = notePayload
  backlinks.value = backlinksPayload
  recentVisited.value = [slug, ...recentVisited.value.filter((item) => item !== slug)].slice(0, 8)
  persistRecentVisited()
  saveState.value = 'idle'
  if (selectedView.value !== 'ai' && selectedView.value !== 'graph') {
    selectedView.value = 'notes'
  }
}

function updateCurrentTitle(value: string) {
  if (!currentNote.value) {
    return
  }
  currentNote.value = {
    ...currentNote.value,
    title: value,
  }
}

function updateCurrentContent(value: string) {
  if (!currentNote.value) {
    return
  }
  currentNote.value = {
    ...currentNote.value,
    content: value,
  }
}

function updateCurrentContentAndRestoreSelection(value: string, selectionStart: number, selectionEnd = selectionStart) {
  updateCurrentContent(value)
  queueAutosave()
  void nextTick(() => {
    const editor = markdownEditor.value
    if (!editor) {
      return
    }
    editor.focus()
    editor.setSelectionRange(selectionStart, selectionEnd)
  })
}

function wrapSelection(prefix: string, suffix: string, placeholder: string) {
  if (!currentNote.value) {
    return
  }
  const editor = markdownEditor.value
  const start = editor?.selectionStart ?? currentNote.value.content.length
  const end = editor?.selectionEnd ?? currentNote.value.content.length
  const selected = currentNote.value.content.slice(start, end)
  const replacement = `${prefix}${selected || placeholder}${suffix}`
  const nextContent = `${currentNote.value.content.slice(0, start)}${replacement}${currentNote.value.content.slice(end)}`
  const rangeStart = start + prefix.length
  const rangeEnd = rangeStart + (selected || placeholder).length
  updateCurrentContentAndRestoreSelection(nextContent, rangeStart, rangeEnd)
}

function transformSelectedLines(transform: (line: string) => string) {
  if (!currentNote.value) {
    return
  }
  const editor = markdownEditor.value
  const start = editor?.selectionStart ?? 0
  const end = editor?.selectionEnd ?? start
  const content = currentNote.value.content
  const lineStart = content.lastIndexOf('\n', Math.max(0, start - 1)) + 1
  const lineEndIndex = content.indexOf('\n', end)
  const lineEnd = lineEndIndex === -1 ? content.length : lineEndIndex
  const selectedBlock = content.slice(lineStart, lineEnd)
  const replacement = selectedBlock
    .split('\n')
    .map((line) => transform(line || '内容'))
    .join('\n')
  const nextContent = `${content.slice(0, lineStart)}${replacement}${content.slice(lineEnd)}`
  updateCurrentContentAndRestoreSelection(nextContent, lineStart, lineStart + replacement.length)
}

function insertSnippet(snippet: string, selectionOffset = 0) {
  if (!currentNote.value) {
    return
  }
  const editor = markdownEditor.value
  const start = editor?.selectionStart ?? currentNote.value.content.length
  const end = editor?.selectionEnd ?? currentNote.value.content.length
  const nextContent = `${currentNote.value.content.slice(0, start)}${snippet}${currentNote.value.content.slice(end)}`
  const cursor = start + selectionOffset
  updateCurrentContentAndRestoreSelection(nextContent, cursor, cursor)
}

function applyMarkdown(action: 'h1' | 'h2' | 'bold' | 'italic' | 'quote' | 'code' | 'link' | 'bullet' | 'todo' | 'table' | 'divider') {
  switch (action) {
    case 'h1':
      transformSelectedLines((line) => `# ${line.replace(/^#{1,6}\s+/, '')}`)
      return
    case 'h2':
      transformSelectedLines((line) => `## ${line.replace(/^#{1,6}\s+/, '')}`)
      return
    case 'bold':
      wrapSelection('**', '**', '重点内容')
      return
    case 'italic':
      wrapSelection('*', '*', '强调内容')
      return
    case 'quote':
      transformSelectedLines((line) => `> ${line.replace(/^>\s?/, '')}`)
      return
    case 'code':
      wrapSelection('```\n', '\n```', 'code block')
      return
    case 'link':
      wrapSelection('[', '](https://example.com)', '链接文本')
      return
    case 'bullet':
      transformSelectedLines((line) => `- ${line.replace(/^[-*+]\s+/, '')}`)
      return
    case 'todo':
      transformSelectedLines((line) => `- [ ] ${line.replace(/^- \[[ x]\]\s*/, '')}`)
      return
    case 'table':
      insertSnippet('| 字段 | 内容 |\n| --- | --- |\n| 标题 | 说明 |', 2)
      return
    case 'divider':
      insertSnippet('\n---\n', 5)
      return
  }
}

function jumpToOutline(line: number) {
  if (!currentNote.value) {
    return
  }
  const lines = currentNote.value.content.split('\n')
  const cursor = lines.slice(0, line).reduce((total, item) => total + item.length + 1, 0)
  void nextTick(() => {
    const editor = markdownEditor.value
    if (!editor) {
      return
    }
    editor.focus()
    editor.setSelectionRange(cursor, cursor)
  })
}

function markDirty() {
  saveState.value = 'dirty'
}

function queueAutosave() {
  if (!currentNote.value) {
    return
  }
  markDirty()
  if (autosaveTimer) {
    window.clearTimeout(autosaveTimer)
  }
  autosaveTimer = window.setTimeout(() => {
    void saveCurrentNote()
  }, 1200)
}

async function saveCurrentNote() {
  if (!currentNote.value) {
    return
  }
  saveState.value = 'saving'
  try {
    const payload = await requestJson<NoteRecord>(`/notes/${currentNote.value.slug}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: currentNote.value.title,
        content: currentNote.value.content,
        path: currentNote.value.path,
        tags: currentNote.value.tags,
        links: currentNote.value.links,
        previous_version: currentNote.value.version,
      }),
    }, '保存失败')
    currentNote.value = payload
    await refreshCollections(payload.slug)
    saveState.value = 'saved'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败'
    saveState.value = 'error'
  }
}

async function createQuickNote() {
  const now = new Date().toISOString().slice(0, 16).replace('T', ' ')
  const title = `新笔记 ${now}`
  const slug = title.toLowerCase().replace(/[^\w\u4e00-\u9fff-]+/g, '-')
  const payload = await requestJson<NoteRecord>('/notes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title,
      slug,
      content: `# ${title}\n\n在这里输入内容。`,
      path: `01_Notes/${title}.md`,
      tags: [],
      links: [],
      source_url: '',
    }),
  }, '创建笔记失败')
  await refreshCollections(payload.slug)
  selectedView.value = 'notes'
}

async function createFolder() {
  if (!folderPath.value.trim()) {
    return
  }
  await requestJson('/folders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: folderPath.value.trim() }),
  }, '创建文件夹失败')
  directoryAction.value = null
  await refreshCollections(currentNote.value?.slug)
}

async function renameFolder() {
  if (!folderSourcePath.value.trim() || !folderTargetPath.value.trim()) {
    return
  }
  await requestJson('/folders/move', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      source_path: folderSourcePath.value.trim(),
      target_path: folderTargetPath.value.trim(),
    }),
  }, '重命名文件夹失败')
  directoryAction.value = null
  await refreshCollections(currentNote.value?.slug)
}

async function moveCurrentNote() {
  if (!currentNote.value || !noteTargetPath.value.trim()) {
    return
  }
  const payload = await requestJson<NoteRecord>(`/notes/${currentNote.value.slug}/move`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      target_path: noteTargetPath.value.trim(),
      previous_version: currentNote.value.version,
    }),
  }, '移动笔记失败')
  directoryAction.value = null
  currentNote.value = payload
  await refreshCollections(payload.slug)
}

async function deleteCurrentNote() {
  if (!currentNote.value) {
    return
  }
  try {
    await requestJson(`/notes/${currentNote.value.slug}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ previous_version: currentNote.value.version }),
    }, '删除失败')
    currentNote.value = null
    aiResponse.value = ''
    aiReferences.value = []
    saveState.value = 'idle'
    await refreshCollections(notes.value[0]?.slug)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除失败'
    saveState.value = 'error'
  }
}

function openDirectoryAction(action: Exclude<DirectoryAction, null>) {
  directoryAction.value = action
  if (action === 'create-folder') {
    folderPath.value = currentFolderPath.value || '01_Notes/'
    return
  }
  if (action === 'rename-folder') {
    folderSourcePath.value = currentFolderPath.value
    folderTargetPath.value = currentFolderPath.value
    return
  }
  noteTargetPath.value = currentNote.value?.path ?? ''
}

function closeDirectoryAction() {
  directoryAction.value = null
}

async function performSearch() {
  if (searchQuery.value.trim().length < 2) {
    searchResults.value = []
    return
  }
  try {
    const payload = await requestJson<{ results: SearchResult[] }>(`/search?q=${encodeURIComponent(searchQuery.value)}`, {}, '搜索失败')
    searchResults.value = payload.results
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '搜索失败'
  }
}

async function submitClip() {
  if (!clipUrl.value.trim()) {
    return
  }
  isBusy.value = true
  errorMessage.value = ''
  try {
    const payload = await requestJson<ClipPreviewResult>('/clip', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url: clipUrl.value,
        device_id: deviceId,
        summarize_with_ai: clipSummarizeWithAi.value,
        use_ai: useRemoteAi.value,
        use_rag: useRag.value,
        summary_prompt: clipSummaryPrompt.value,
        target_folder: '00_Inbox',
        save_to_note: false,
      }),
    }, '解析失败，请确认目标网址可访问。')
    clipPreview.value = payload
    showClipPreview.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '解析失败'
  } finally {
    isBusy.value = false
  }
}

function closeClipPreview() {
  showClipPreview.value = false
  clipPreview.value = null
}

async function saveClipPreview() {
  if (!clipPreview.value) {
    return
  }
  isBusy.value = true
  errorMessage.value = ''
  try {
    const payload = await requestJson<{ note?: NoteRecord | null }>('/clip/save-preview', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: clipPreview.value.extracted_title,
        source_url: clipPreview.value.source_url,
        content: clipPreview.value.preview_content,
        summary: clipPreview.value.summary,
        target_folder: '00_Inbox',
        device_id: deviceId,
      }),
    }, '保存笔记失败')
    clipUrl.value = ''
    closeClipPreview()
    if (payload.note?.slug) {
      await refreshCollections(payload.note.slug)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存笔记失败'
  } finally {
    isBusy.value = false
  }
}

async function loadAiProviderCatalog() {
  if (!token.value) {
    return
  }
  try {
    const payload = await requestJson<{ presets: AiProviderPreset[]; current: AiProviderConfig | null }>('/ai/providers/catalog', {}, '获取模型配置失败')
    providerPresets.value = payload.presets
    providerCurrent.value = payload.current
    if (payload.current) {
      providerForm.value.provider_id = payload.current.provider_id
      providerForm.value.provider_label = payload.current.provider_label
      providerForm.value.base_url = payload.current.base_url
      providerForm.value.model_name = payload.current.model_name
      providerForm.value.is_enabled = payload.current.is_enabled
    } else if (payload.presets[0]) {
      applyProviderPreset(payload.presets[0].provider_id)
    }
  } catch {
    // 保持当前配置
  }
}

function applyProviderPreset(providerId: string) {
  const preset = providerPresets.value.find((item) => item.provider_id === providerId)
  if (!preset) {
    return
  }
  providerForm.value.provider_id = preset.provider_id
  providerForm.value.provider_label = preset.label
  providerForm.value.base_url = preset.base_url
  providerForm.value.model_name = preset.models[0] ?? ''
}

async function saveProviderConfig() {
  errorMessage.value = ''
  try {
    const payload = await requestJson<AiProviderConfig>('/ai/providers/current', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        provider_id: providerForm.value.provider_id || 'custom',
        provider_label: providerForm.value.provider_label || '自定义',
        base_url: providerForm.value.base_url,
        api_key: providerForm.value.api_key,
        model_name: providerForm.value.model_name,
        is_enabled: providerForm.value.is_enabled,
      }),
    }, '模型配置保存失败')
    providerCurrent.value = payload
    providerForm.value.provider_id = payload.provider_id
    providerForm.value.provider_label = payload.provider_label
    providerForm.value.base_url = payload.base_url
    providerForm.value.model_name = payload.model_name
    providerForm.value.api_key = ''
    providerForm.value.is_enabled = payload.is_enabled
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '模型配置保存失败'
  }
}

async function uploadDocument(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) {
    return
  }
  const form = new FormData()
  form.append('file', file)
  form.append('summarize_with_ai', 'true')
  form.append('use_ai', String(useRemoteAi.value))
  form.append('use_rag', String(useRag.value))
  form.append('save_to_note', 'true')
  form.append('target_folder', '03_Resources')
  form.append('summary_prompt', '请提炼这份资料的核心观点、结构脉络和可执行建议。')

  isBusy.value = true
  errorMessage.value = ''
  try {
    const response = await authorizedFetch('/documents/upload', {
      method: 'POST',
      body: form,
    })
    const payload = await parseJsonSafe<UploadedDocument | { detail?: unknown }>(response)
    if (!response.ok) {
      throw new Error(parseErrorMessage(payload, '文档上传失败'))
    }
    uploadedDocument.value = payload as UploadedDocument
    const savedSlug = uploadedDocument.value.saved_note?.slug
    if (savedSlug) {
      await refreshCollections(savedSlug)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文档上传失败'
  } finally {
    isBusy.value = false
    ;(event.target as HTMLInputElement).value = ''
  }
}

async function runAi(action: AiAction, saveAsNote = false) {
  isBusy.value = true
  errorMessage.value = ''
  try {
    const payload = await requestJson<{ content: string; references: AiReference[]; saved_note?: NoteRecord | null }>('/ai/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        action,
        note_slug: currentNote.value?.slug,
        prompt: aiPrompt.value || '请基于当前笔记和知识库给出结果',
        save_as_note: saveAsNote,
        target_folder: '00_Inbox',
        use_ai: useRemoteAi.value,
        use_rag: useRag.value,
      }),
    }, 'AI 调用失败，请检查服务端配置。')
    aiResponse.value = payload.content
    aiReferences.value = payload.references
    showAiPreview.value = true
    if (payload.saved_note?.slug) {
      await refreshCollections(payload.saved_note.slug)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'AI 调用失败'
  } finally {
    isBusy.value = false
  }
}

function applyAiResultToCurrentNote() {
  if (!currentNote.value || !aiResponse.value) {
    return
  }
  currentNote.value = {
    ...currentNote.value,
    content: `${currentNote.value.content}\n\n---\n\n${aiResponse.value}`,
  }
  showAiPreview.value = false
  queueAutosave()
  selectedView.value = 'notes'
}

function setTagFilter(tag: string) {
  activeTagFilter.value = activeTagFilter.value === tag ? '' : tag
}

function clearTagFilter() {
  activeTagFilter.value = ''
}

function zoomIn() {
  graphZoom.value = Math.min(1.8, Number((graphZoom.value + 0.1).toFixed(2)))
}

function zoomOut() {
  graphZoom.value = Math.max(0.7, Number((graphZoom.value - 0.1).toFixed(2)))
}

function resetZoom() {
  graphZoom.value = 1
}

function toggleExplorerPane() {
  explorerCollapsed.value = !explorerCollapsed.value
}

function toggleInspectorPane() {
  inspectorCollapsed.value = !inspectorCollapsed.value
}

function resizeExplorerPane(delta: number) {
  explorerCollapsed.value = false
  explorerPaneWidth.value = Math.max(240, Math.min(420, explorerPaneWidth.value + delta))
}

function resizeInspectorPane(delta: number) {
  inspectorCollapsed.value = false
  inspectorPaneWidth.value = Math.max(228, Math.min(380, inspectorPaneWidth.value + delta))
}

function handleGraphWheel(event: WheelEvent) {
  if (event.deltaY < 0) {
    zoomIn()
    return
  }
  zoomOut()
}

function normalizeEvent(payload: Record<string, any>): SyncEventRecord {
  return {
    id: Number(payload.id ?? payload.event_id ?? 0),
    note_slug: String(payload.note_slug ?? ''),
    event_type: String(payload.event_type ?? 'unknown'),
    path: String(payload.path ?? ''),
    target_path: String(payload.target_path ?? ''),
    status: payload.status === 'acked' ? 'acked' : 'pending',
    created_at: String(payload.created_at ?? new Date().toISOString()),
    message: '',
  }
}

function mergeEvents(incoming: SyncEventRecord[]) {
  const merged = new Map(events.value.map((event) => [event.id, event]))
  for (const event of incoming) {
    merged.set(event.id, {
      ...(merged.get(event.id) ?? event),
      ...event,
      status: event.status,
      message: event.status === 'acked' ? '' : merged.get(event.id)?.message ?? '',
    })
  }
  events.value = [...merged.values()].sort((left, right) => right.id - left.id).slice(0, 120)
}

function updateEventState(eventId: number, status: SyncEventState, message = '') {
  events.value = events.value.map((event) =>
    event.id === eventId
      ? {
          ...event,
          status,
          message,
        }
      : event,
  )
}

async function invokeDesktop<T>(command: string, args: Record<string, unknown>) {
  if (!hasTauriRuntime.value) {
    throw new Error('当前运行在浏览器预览模式，无法操作本地文件系统。')
  }
  return invoke<T>(command, args)
}

async function validateRootPath(triggerSync = true) {
  errorMessage.value = ''
  try {
    const resolved = await invokeDesktop<string>('ensure_sync_root', { rootPath: desktopRootPath.value })
    desktopRootPath.value = resolved
    agentMessage.value = `本地副本目录已就绪：${resolved}`
    if (triggerSync && autoSyncEnabled.value) {
      await processPendingEvents()
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '本地副本目录不可用'
  }
}

async function fetchNote(slug: string): Promise<NoteRecord> {
  return requestJson<NoteRecord>(`/notes/${slug}`, {}, `无法获取笔记 ${slug}`)
}

async function ackEvents(eventIds: number[]) {
  if (!token.value || eventIds.length === 0) {
    return
  }
  await requestJson('/sync/ack', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ event_ids: eventIds, device_id: deviceId }),
  }, '同步确认失败')
  events.value = events.value.map((event) =>
    eventIds.includes(event.id)
      ? {
          ...event,
          status: 'acked',
          message: '',
        }
      : event,
  )
  await refreshStatus()
}

async function applyEvent(event: SyncEventRecord) {
  switch (event.event_type) {
    case 'upsert': {
      if (!event.note_slug) {
        throw new Error('缺少 note_slug，无法写入本地副本')
      }
      const note = await fetchNote(event.note_slug)
      await invokeDesktop<string>('write_sync_file', {
        rootPath: desktopRootPath.value,
        relativePath: note.path,
        content: note.content,
      })
      return
    }
    case 'move': {
      try {
        await invokeDesktop<string>('move_sync_path', {
          rootPath: desktopRootPath.value,
          sourcePath: event.path,
          targetPath: event.target_path,
        })
      } catch {
        if (!event.note_slug) {
          throw new Error('本地缺少源文件，且无法回拉笔记内容')
        }
        const note = await fetchNote(event.note_slug)
        await invokeDesktop<string>('write_sync_file', {
          rootPath: desktopRootPath.value,
          relativePath: note.path,
          content: note.content,
        })
        await invokeDesktop<string>('delete_sync_path', {
          rootPath: desktopRootPath.value,
          relativePath: event.path,
        })
      }
      return
    }
    case 'delete': {
      await invokeDesktop<string>('delete_sync_path', {
        rootPath: desktopRootPath.value,
        relativePath: event.path,
      })
      return
    }
    case 'folder-create': {
      await invokeDesktop<string>('ensure_sync_directory', {
        rootPath: desktopRootPath.value,
        relativePath: event.path,
      })
      return
    }
    case 'folder-move': {
      await invokeDesktop<string>('move_sync_path', {
        rootPath: desktopRootPath.value,
        sourcePath: event.path,
        targetPath: event.target_path,
      })
      return
    }
    case 'folder-delete': {
      await invokeDesktop<string>('delete_sync_path', {
        rootPath: desktopRootPath.value,
        relativePath: event.path,
      })
      return
    }
    default:
      throw new Error(`暂不支持的事件类型：${event.event_type}`)
  }
}

async function processPendingEvents() {
  if (!token.value || !autoSyncEnabled.value || !desktopRootPath.value.trim() || !hasTauriRuntime.value) {
    return
  }
  if (isApplyingEvents.value) {
    reprocessRequested = true
    return
  }

  isApplyingEvents.value = true
  try {
    await validateRootPath(false)
    do {
      reprocessRequested = false
      const pending = [...events.value].filter((event) => event.status === 'pending').sort((left, right) => left.id - right.id)
      for (const event of pending) {
        updateEventState(event.id, 'applying')
        try {
          await applyEvent(event)
          await ackEvents([event.id])
          lastSyncedAt.value = new Date().toLocaleString('zh-CN')
          agentMessage.value = `已同步到本地副本：#${event.id} ${event.event_type}`
        } catch (error) {
          const message = error instanceof Error ? error.message : '同步事件执行失败'
          updateEventState(event.id, 'error', message)
          errorMessage.value = message
        }
      }
    } while (reprocessRequested)
  } finally {
    isApplyingEvents.value = false
  }
}

async function refreshEvents(reset = false) {
  if (!token.value) {
    return
  }
  try {
    const afterId = reset ? 0 : Math.max(0, ...events.value.map((event) => event.id))
    const payload = await requestJson<Array<Record<string, any>>>(`/sync/events?device_id=${deviceId}&after_id=${afterId}`, {}, '获取同步事件失败')
    if (reset) {
      events.value = []
    }
    if (payload.length > 0) {
      mergeEvents(payload.map((item) => normalizeEvent(item)))
      await processPendingEvents()
    }
  } catch {
    // 保持现状
  }
}

async function retryFailedEvents() {
  events.value = events.value.map((event) =>
    event.status === 'error'
      ? {
          ...event,
          status: 'pending',
          message: '',
        }
      : event,
  )
  await processPendingEvents()
}

async function ackVisibleEvents() {
  const visibleIds = events.value.filter((event) => event.status !== 'acked').map((event) => event.id)
  await ackEvents(visibleIds)
}

function connectSyncSocket() {
  if (!token.value) {
    return
  }
  syncSocket.value?.close()
  const wsBase = apiBase.replace('http://', 'ws://').replace('https://', 'wss://').replace('/api/v1', '')
  const socket = new WebSocket(`${wsBase}/api/v1/sync/ws/${deviceId}?token=${encodeURIComponent(token.value)}`)
  socket.onmessage = async (event) => {
    mergeEvents([normalizeEvent(JSON.parse(event.data) as Record<string, any>)])
    await refreshStatus()
    await processPendingEvents()
  }
  socket.onclose = () => {
    syncSocket.value = null
  }
  syncSocket.value = socket
}

function logout() {
  clearToken()
  currentUser.value = null
  notes.value = []
  currentNote.value = null
  backlinks.value = { note_slug: '', incoming: [], outgoing: [] }
  tags.value = []
  searchResults.value = []
  selectedView.value = 'home'
  openLoginModal('manual')
}

onMounted(async () => {
  agentMessage.value = hasTauriRuntime.value
    ? '桌面代理已加载，等待连接云端与校验本地副本目录。'
    : '当前为浏览器预览模式，仅可查看事件，无法写入本地文件。'

  await bootstrap()
  if (token.value) {
    await refreshEvents(true)
  }

  pollTimer = window.setInterval(() => {
    void refreshStatus()
    void refreshEvents()
  }, 4000)
})

onUnmounted(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
  }
  if (autosaveTimer) {
    window.clearTimeout(autosaveTimer)
  }
  syncSocket.value?.close()
})
</script>

<template>
  <main class="desktop-workspace">
    <aside class="workspace-sidebar workspace-sidebar--obsidian panel-shell">
      <div class="brand-block brand-block--obsidian">
        <div class="brand-glyph">K</div>
      </div>

      <nav class="sidebar-nav sidebar-nav--icons">
        <button
          v-for="item in navItems"
          :key="item.id"
          class="icon-button icon-button--rail"
          :class="{ 'icon-button--active': selectedView === item.id }"
          type="button"
          :title="item.label"
          @click="openView(item.id)"
        >
          <svg v-if="item.id === 'home'" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 11.5 12 5l8 6.5" /><path d="M7 10.5V19h10v-8.5" /></svg>
          <svg v-else-if="item.id === 'notes'" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4.5h8l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5z" /><path d="M15 4.5V9h4" /></svg>
          <svg v-else-if="item.id === 'graph'" viewBox="0 0 24 24" aria-hidden="true"><circle cx="6.5" cy="7" r="2.5" /><circle cx="17.5" cy="6.5" r="2.5" /><circle cx="12" cy="17" r="3" /><path d="M8.8 8.2 10.5 14" /><path d="M15.2 8l-1.7 6" /><path d="M9 7h6" /></svg>
          <svg v-else-if="item.id === 'ai'" viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /><path d="m18.5 14 0.8 2.2 2.2 0.8-2.2 0.8-0.8 2.2-0.8-2.2-2.2-0.8 2.2-0.8z" /></svg>
          <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M10.3 2.8h3.4l.5 2.2a7.6 7.6 0 0 1 1.8.8l2-1.1 2.4 2.4-1.1 2a7.6 7.6 0 0 1 .8 1.8l2.2.5v3.4l-2.2.5a7.6 7.6 0 0 1-.8 1.8l1.1 2-2.4 2.4-2-1.1a7.6 7.6 0 0 1-1.8.8l-.5 2.2h-3.4l-.5-2.2a7.6 7.6 0 0 1-1.8-.8l-2 1.1-2.4-2.4 1.1-2a7.6 7.6 0 0 1-.8-1.8l-2.2-.5v-3.4l2.2-.5a7.6 7.6 0 0 1 .8-1.8l-1.1-2 2.4-2.4 2 1.1a7.6 7.6 0 0 1 1.8-.8z" /><circle cx="12" cy="12" r="3.1" /></svg>
        </button>
      </nav>
    </aside>

    <section class="workspace-main">
      <header class="workspace-header panel-shell panel-shell--dense">
        <div>
          <p class="eyebrow">{{ navItems.find((item) => item.id === selectedView)?.caption }}</p>
          <h2>{{ navItems.find((item) => item.id === selectedView)?.label }}</h2>
        </div>
        <div class="header-actions">
          <span class="pill">事件 {{ syncStatus.last_event_id ?? 0 }}</span>
          <span class="pill">{{ hasTauriRuntime ? 'Tauri 已连接' : '预览模式' }}</span>
          <button v-if="selectedView !== 'notes'" class="icon-button" type="button" title="打开编辑区" @click="openView('notes')">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4.5h8l4 4V19H5V6A1.5 1.5 0 0 1 6.5 4.5z" /><path d="M15 4.5V9h4" /></svg>
          </button>
          <button class="account-button" type="button" :title="isConnected ? '查看账户信息' : '登录账户'" @click="openLoginModal(isConnected ? 'manual' : 'initial')">
            <span class="account-button__avatar">{{ displayUserInitial }}</span>
            <span class="account-button__copy">
              <strong>{{ displayUserName }}</strong>
              <small>{{ displayUserMeta }}</small>
            </span>
          </button>
        </div>
      </header>

      <section v-if="selectedView === 'home'" class="home-layout">
        <section class="panel-shell hero-panel hero-panel--compact">
          <div>
            <p class="eyebrow">Workspace</p>
            <h3>{{ instanceConfig?.instance_name || '个人双向知识库' }}</h3>
            <p class="hero-copy">{{ displayUserName }} · {{ instanceConfig?.deployment_mode === 'server' ? '服务器部署' : '主电脑部署' }} · {{ lastSyncedAt || '尚未同步' }}</p>
          </div>
          <div class="hero-actions">
            <button class="icon-button" type="button" title="打开笔记" @click="openView('notes')">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4.5h8l4 4V19H5V6A1.5 1.5 0 0 1 6.5 4.5z" /><path d="M15 4.5V9h4" /></svg>
            </button>
            <button class="icon-button" type="button" title="账户" @click="openLoginModal('manual')">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" /><path d="M4.5 19a8.5 8.5 0 0 1 15 0" /></svg>
            </button>
          </div>
        </section>

        <section class="home-overview-grid">
          <article v-for="item in homeMetrics" :key="item.label" class="metric-card metric-card--dense">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </section>

        <div class="home-grid">
          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Search</p>
                <h3>搜索</h3>
              </div>
              <button class="icon-button" type="button" title="搜索" @click="performSearch"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6" /><path d="m20 20-4.2-4.2" /></svg></button>
            </header>
            <input
              v-model="searchQuery"
              class="field-input field-input--large"
              placeholder="搜索标题、正文或标签"
              @keyup.enter="performSearch"
            />
            <div class="result-list">
              <button
                v-for="item in searchResults"
                :key="item.note_slug"
                class="result-card"
                type="button"
                @click="selectNote(item.note_slug)"
              >
                <strong>{{ item.title }}</strong>
                <span>{{ item.path }}</span>
                <p>{{ item.snippet }}</p>
              </button>
            </div>
          </section>

          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Clipper</p>
                <h3>保存链接</h3>
              </div>
              <button class="icon-button icon-button--primary" type="button" :disabled="isBusy" :title="isBusy ? '解析中' : '解析链接'" @click="submitClip"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 14 21 3" /><path d="M21 3h-6" /><path d="M21 3v6" /><path d="M14 10 3 21" /></svg></button>
            </header>
            <input v-model="clipUrl" class="field-input" placeholder="粘贴链接后直接解析" />
            <label class="toggle-chip">
              <input v-model="clipSummarizeWithAi" type="checkbox" />
              <span>AI 提炼总结</span>
            </label>
            <textarea
              v-if="clipSummarizeWithAi"
              v-model="clipSummaryPrompt"
              class="field-input textarea-input textarea-input--compact"
              placeholder="提炼要点与结构化总结"
            />
          </section>

          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Recent</p>
                <h3>最近打开</h3>
              </div>
              <span class="pill">{{ homeRecentNotes.length }}</span>
            </header>
            <div class="recent-list">
              <button v-for="note in homeRecentNotes" :key="note.slug" class="row-card" type="button" @click="selectNote(note.slug)">
                <strong>{{ note.title }}</strong>
                <span>{{ note.path }}</span>
              </button>
            </div>
          </section>

          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Status</p>
                <h3>同步状态</h3>
              </div>
              <span class="pill">本地 {{ pendingLocalEvents }}</span>
            </header>
            <div class="status-list">
              <div class="property-row">
                <span>账户</span>
                <strong>{{ displayUserName }}</strong>
              </div>
              <div class="property-row">
                <span>副本目录</span>
                <strong>{{ desktopRootPath }}</strong>
              </div>
              <div class="property-row">
                <span>最近同步</span>
                <strong>{{ lastSyncedAt || '尚未同步' }}</strong>
              </div>
              <div class="property-row">
                <span>事件 ID</span>
                <strong>{{ syncStatus.last_event_id ?? 0 }}</strong>
              </div>
            </div>
          </section>
        </div>
      </section>

      <section v-else-if="selectedView === 'notes'" class="notes-layout notes-layout--obsidian" :style="notesLayoutStyle">
        <section class="panel-shell note-directory-panel" :class="{ 'pane-collapsed': explorerCollapsed }">
          <header class="section-header">
            <div>
              <p class="eyebrow">Workspace</p>
              <h3>{{ explorerCollapsed ? '目录' : '文件树' }}</h3>
            </div>
            <div class="action-row action-row--iconbar">
              <button class="icon-button" type="button" title="收起目录" @click="toggleExplorerPane">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 6 9 12l6 6" /></svg>
              </button>
              <button class="icon-button" type="button" title="缩小目录" @click="resizeExplorerPane(-32)">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 12h12" /></svg>
              </button>
              <button class="icon-button" type="button" title="放大目录" @click="resizeExplorerPane(32)">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v12" /><path d="M6 12h12" /></svg>
              </button>
              <button class="icon-button" type="button" title="新建文件夹" @click="openDirectoryAction('create-folder')">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H10l1.6 2H18.5A2.5 2.5 0 0 1 21 9.5v7A2.5 2.5 0 0 1 18.5 19h-13A2.5 2.5 0 0 1 3 16.5z" /><path d="M12 10v6" /><path d="M9 13h6" /></svg>
              </button>
              <button class="icon-button" type="button" title="重命名文件夹" @click="openDirectoryAction('rename-folder')">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7.5A2.5 2.5 0 0 1 6.5 5H10l1.6 2H18.5A2.5 2.5 0 0 1 21 9.5v7A2.5 2.5 0 0 1 18.5 19h-12A2.5 2.5 0 0 1 4 16.5z" /><path d="m10 15 5.8-5.8 1.9 1.9L12 17H10z" /></svg>
              </button>
              <button class="icon-button icon-button--primary" type="button" title="新建笔记" @click="createQuickNote">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg>
              </button>
            </div>
          </header>

          <div v-if="directoryAction" class="inline-card">
            <div class="inline-card__header">
              <strong>
                {{
                  directoryAction === 'create-folder'
                    ? '新建文件夹'
                    : directoryAction === 'rename-folder'
                      ? '重命名文件夹'
                      : '移动当前笔记'
                }}
              </strong>
              <button class="ghost-button ghost-button--small" type="button" @click="closeDirectoryAction">关闭</button>
            </div>

            <div v-if="directoryAction === 'create-folder'" class="inline-grid">
              <input v-model="folderPath" class="field-input" placeholder="例如 01_Notes/项目笔记" />
              <button class="primary-button primary-button--small" type="button" @click="createFolder">确认创建</button>
            </div>

            <div v-else-if="directoryAction === 'rename-folder'" class="inline-grid inline-grid--double">
              <input v-model="folderSourcePath" class="field-input" placeholder="当前路径" />
              <input v-model="folderTargetPath" class="field-input" placeholder="新路径" />
              <button class="primary-button primary-button--small" type="button" @click="renameFolder">确认重命名</button>
            </div>

            <div v-else class="inline-grid">
              <input v-model="noteTargetPath" class="field-input" placeholder="例如 02_Articles/system-overview.md" />
              <button class="primary-button primary-button--small" type="button" @click="moveCurrentNote">确认移动</button>
            </div>
          </div>

          <div class="tag-row">
            <button class="icon-button" type="button" title="清除标签筛选" @click="clearTagFilter">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14" /></svg>
            </button>
            <button
              v-for="tag in visibleTags"
              :key="tag.tag"
              class="tag-chip"
              :class="{ 'tag-chip--active': activeTagFilter === tag.tag }"
              type="button"
              @click="setTagFilter(tag.tag)"
            >
              #{{ tag.tag }} {{ tag.count }}
            </button>
          </div>

          <div v-if="!explorerCollapsed" class="directory-groups">
            <section v-for="group in groupedNotes" :key="group.folder" class="directory-group">
              <div class="directory-group__header">
                <span>{{ group.folder }}</span>
                <small>{{ group.notes.length }} 篇</small>
              </div>
              <button
                v-for="note in group.notes"
                :key="note.slug"
                class="note-list-card"
                :class="{ 'note-list-card--active': currentNote?.slug === note.slug }"
                type="button"
                @click="selectNote(note.slug)"
              >
                <strong>{{ note.title }}</strong>
                <p>{{ note.summary }}</p>
              </button>
            </section>
          </div>
          <div v-else class="collapsed-pane-actions">
            <button
              v-for="note in filteredNotes.slice(0, 8)"
              :key="note.slug"
              class="icon-button icon-button--stack"
              :class="{ 'icon-button--active': currentNote?.slug === note.slug }"
              type="button"
              :title="note.title"
              @click="selectNote(note.slug)"
            >
              <span>{{ note.title.slice(0, 1) }}</span>
            </button>
          </div>
        </section>

        <section class="panel-shell note-editor-panel">
          <header class="section-header section-header--editor">
            <div class="editor-headline">
              <input
                :value="currentNote?.title ?? ''"
                class="title-input"
                placeholder="笔记标题"
                @input="updateCurrentTitle(($event.target as HTMLInputElement).value); queueAutosave()"
              />
              <span class="muted-text">{{ currentNote?.path ?? '从左侧目录选择一篇笔记开始编辑' }}</span>
            </div>
            <div class="action-row action-row--tight action-row--iconbar">
              <span class="pill">{{ saveLabel }}</span>
              <button class="icon-button" type="button" title="移动当前笔记" :disabled="!currentNote" @click="openDirectoryAction('move-note')">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 8 3 12l4 4" /><path d="M17 8l4 4-4 4" /><path d="M4 12h16" /></svg>
              </button>
              <button class="icon-button" type="button" title="立即保存" :disabled="!currentNote" @click="saveCurrentNote">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4.5h12l2 2V19H5z" /><path d="M8 4.5V9h8V4.5" /><path d="M9 19v-6h6v6" /></svg>
              </button>
              <button class="icon-button icon-button--danger" type="button" title="删除笔记" :disabled="!currentNote" @click="deleteCurrentNote">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16" /><path d="M9 7V5h6v2" /><path d="M7 7l1 12h8l1-12" /></svg>
              </button>
            </div>
          </header>

          <div class="editor-toolbar editor-toolbar--icons">
            <button class="icon-button toolbar-button" type="button" title="H1" :disabled="!currentNote" @click="applyMarkdown('h1')"><span>H1</span></button>
            <button class="icon-button toolbar-button" type="button" title="H2" :disabled="!currentNote" @click="applyMarkdown('h2')"><span>H2</span></button>
            <button class="icon-button toolbar-button" type="button" title="粗体" :disabled="!currentNote" @click="applyMarkdown('bold')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5h5a3 3 0 0 1 0 6H8z" /><path d="M8 11h6a3.5 3.5 0 0 1 0 7H8z" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="斜体" :disabled="!currentNote" @click="applyMarkdown('italic')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 5h5" /><path d="M5 19h5" /><path d="M14 5 10 19" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="引用" :disabled="!currentNote" @click="applyMarkdown('quote')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 8h4v4H7z" /><path d="M13 8h4v4h-4z" /><path d="M7 12v4h4" /><path d="M13 12v4h4" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="代码块" :disabled="!currentNote" @click="applyMarkdown('code')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m8 8-4 4 4 4" /><path d="m16 8 4 4-4 4" /><path d="M13 5 11 19" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="链接" :disabled="!currentNote" @click="applyMarkdown('link')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 14 21 3" /><path d="M21 3h-6" /><path d="M21 3v6" /><path d="M14 10 3 21" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="列表" :disabled="!currentNote" @click="applyMarkdown('bullet')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 7h11" /><path d="M9 12h11" /><path d="M9 17h11" /><circle cx="4.5" cy="7" r="1" /><circle cx="4.5" cy="12" r="1" /><circle cx="4.5" cy="17" r="1" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="待办" :disabled="!currentNote" @click="applyMarkdown('todo')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 7h11" /><path d="M9 12h11" /><path d="M9 17h11" /><path d="m3.5 7 1.5 1.5L7.5 6" /><rect x="3" y="10" width="3" height="3" rx=".4" /><rect x="3" y="15" width="3" height="3" rx=".4" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="表格" :disabled="!currentNote" @click="applyMarkdown('table')"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="5" width="16" height="14" rx="1" /><path d="M4 10h16" /><path d="M10 5v14" /></svg></button>
            <button class="icon-button toolbar-button" type="button" title="分隔线" :disabled="!currentNote" @click="applyMarkdown('divider')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 12h16" /></svg></button>
          </div>

          <textarea
            ref="markdownEditor"
            :value="currentNote?.content ?? ''"
            class="editor-textarea"
            placeholder="输入 Markdown 内容，停止输入后会自动保存。"
            @input="updateCurrentContent(($event.target as HTMLTextAreaElement).value); queueAutosave()"
          />

          <div class="editor-footer">
            <span class="helper-text">{{ noteWordCount }} 词</span>
            <span class="helper-text">{{ noteCharacterCount }} 字符</span>
            <span class="helper-text">预计阅读 {{ noteReadingMinutes }} 分钟</span>
            <span class="helper-text">大纲 {{ noteOutline.length }} 项</span>
          </div>
        </section>

        <aside class="notes-aside" :class="{ 'pane-collapsed': inspectorCollapsed }">
          <section class="panel-shell panel-shell--compact">
            <p class="eyebrow">Inspector</p>
            <div class="section-header section-header--compact">
              <h3>{{ inspectorCollapsed ? '检查' : (currentNote?.title ?? '未选择笔记') }}</h3>
              <div class="action-row action-row--iconbar">
                <button class="icon-button" type="button" title="缩小侧栏" @click="resizeInspectorPane(-28)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 12h12" /></svg></button>
                <button class="icon-button" type="button" title="放大侧栏" @click="resizeInspectorPane(28)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v12" /><path d="M6 12h12" /></svg></button>
                <button class="icon-button" type="button" title="收起右侧栏" @click="toggleInspectorPane"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" /></svg></button>
              </div>
            </div>
            <div class="stats-grid stats-grid--compact">
              <article class="metric-card metric-card--plain">
                <span>出链</span>
                <strong>{{ backlinks.outgoing.length }}</strong>
              </article>
              <article class="metric-card metric-card--plain">
                <span>反链</span>
                <strong>{{ backlinks.incoming.length }}</strong>
              </article>
              <article class="metric-card metric-card--plain">
                <span>标签</span>
                <strong>{{ currentNote?.tags.length ?? 0 }}</strong>
              </article>
            </div>
          </section>

          <section v-if="!inspectorCollapsed" class="panel-shell panel-shell--compact">
            <div class="inspector-tabs inspector-tabs--icons">
              <button class="tab-button tab-button--icon" :class="{ 'tab-button--active': rightTab === 'outline' }" type="button" title="目录" @click="rightTab = 'outline'"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 7h12" /><path d="M6 12h8" /><path d="M6 17h10" /></svg></button>
              <button class="tab-button tab-button--icon" :class="{ 'tab-button--active': rightTab === 'links' }" type="button" title="链接" @click="rightTab = 'links'"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 14 21 3" /><path d="M21 3h-6" /><path d="M21 3v6" /><path d="M14 10 3 21" /></svg></button>
              <button class="tab-button tab-button--icon" :class="{ 'tab-button--active': rightTab === 'tags' }" type="button" title="标签" @click="rightTab = 'tags'"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 12V5h7l9 9-7 7z" /><circle cx="8.5" cy="8.5" r="1" /></svg></button>
              <button class="tab-button tab-button--icon" :class="{ 'tab-button--active': rightTab === 'properties' }" type="button" title="属性" @click="rightTab = 'properties'"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v18" /><path d="M5 8h14" /><path d="M7 16h10" /></svg></button>
              <button class="tab-button tab-button--icon" :class="{ 'tab-button--active': rightTab === 'ai' }" type="button" title="AI" @click="rightTab = 'ai'"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /></svg></button>
            </div>

            <div v-if="rightTab === 'outline'" class="inspector-section">
              <button
                v-for="item in noteOutline"
                :key="`${item.line}-${item.text}`"
                class="outline-item"
                type="button"
                :style="{ paddingLeft: `${12 + (item.level - 1) * 14}px` }"
                @click="jumpToOutline(item.line)"
              >
                {{ item.text }}
              </button>
              <p v-if="noteOutline.length === 0" class="helper-text">暂无目录</p>
            </div>

            <div v-else-if="rightTab === 'links'" class="inspector-section inspector-stack">
              <div>
                <p class="eyebrow">Outgoing</p>
                <div class="link-list">
                  <button
                    v-for="item in backlinks.outgoing"
                    :key="`${item.source_slug}-${item.target_slug}`"
                    class="link-button"
                    type="button"
                    @click="selectNote(item.target_slug)"
                  >
                    [[{{ item.target_slug }}]]
                  </button>
                  <p v-if="backlinks.outgoing.length === 0" class="helper-text">暂无出链。</p>
                </div>
              </div>

              <div>
                <p class="eyebrow">Backlinks</p>
                <div class="link-list">
                  <button
                    v-for="item in backlinks.incoming"
                    :key="`${item.source_slug}-${item.target_slug}`"
                    class="link-button"
                    type="button"
                    @click="selectNote(item.source_slug)"
                  >
                    {{ item.source_slug }}
                  </button>
                  <p v-if="backlinks.incoming.length === 0" class="helper-text">暂无反链。</p>
                </div>
              </div>
            </div>

            <div v-else-if="rightTab === 'tags'" class="inspector-section inspector-stack">
              <div class="tag-row">
                <span v-for="tag in currentNote?.tags || []" :key="tag" class="pill">#{{ tag }}</span>
              </div>
              <p v-if="(currentNote?.tags.length ?? 0) === 0" class="helper-text">当前笔记暂无标签。</p>
              <div class="tag-row">
                <button
                  v-for="tag in visibleTags"
                  :key="`panel-${tag.tag}`"
                  class="tag-chip"
                  type="button"
                  @click="setTagFilter(tag.tag)"
                >
                  #{{ tag.tag }} {{ tag.count }}
                </button>
              </div>
            </div>

            <div v-else-if="rightTab === 'properties'" class="inspector-section property-list">
              <div class="property-row"><span>路径</span><strong>{{ currentNote?.path || '-' }}</strong></div>
              <div class="property-row"><span>Slug</span><strong>{{ currentNote?.slug || '-' }}</strong></div>
              <div class="property-row"><span>更新于</span><strong>{{ currentNote?.updated_at || '-' }}</strong></div>
              <div class="property-row"><span>版本</span><strong>{{ currentNote?.version || '-' }}</strong></div>
              <div class="property-row"><span>字符数</span><strong>{{ noteCharacterCount }}</strong></div>
              <div class="property-row"><span>阅读时间</span><strong>{{ noteReadingMinutes }} 分钟</strong></div>
            </div>

            <div v-else class="inspector-section inspector-stack">
              <textarea v-model="aiPrompt" class="field-input textarea-input textarea-input--compact" placeholder="例如：总结当前笔记并补充关联知识" />
              <div class="action-row action-row--iconbar">
                <button class="icon-button" type="button" title="总结" @click="runAi('summary')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 7h12" /><path d="M6 12h8" /><path d="M6 17h10" /></svg></button>
                <button class="icon-button" type="button" title="扩写" @click="runAi('expand')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
                <button class="icon-button" type="button" title="润色" @click="runAi('polish')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 17.5V20h2.5L18 8.5 15.5 6z" /><path d="M13.5 8 16 10.5" /></svg></button>
                <button class="icon-button icon-button--primary" type="button" title="创作并保存" @click="runAi('create', true)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /></svg></button>
              </div>
              <pre class="ai-output">{{ aiResponse || 'AI 结果会显示在这里。' }}</pre>
              <button v-if="showAiPreview && aiResponse" class="icon-button icon-button--primary" type="button" title="追加到当前笔记" @click="applyAiResultToCurrentNote">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg>
              </button>
            </div>
          </section>
          <section v-else class="panel-shell panel-shell--compact collapsed-pane-actions">
            <button class="icon-button icon-button--stack" :class="{ 'icon-button--active': rightTab === 'outline' }" type="button" title="目录" @click="rightTab = 'outline'; toggleInspectorPane()"><span>目</span></button>
            <button class="icon-button icon-button--stack" :class="{ 'icon-button--active': rightTab === 'links' }" type="button" title="链接" @click="rightTab = 'links'; toggleInspectorPane()"><span>链</span></button>
            <button class="icon-button icon-button--stack" :class="{ 'icon-button--active': rightTab === 'tags' }" type="button" title="标签" @click="rightTab = 'tags'; toggleInspectorPane()"><span>签</span></button>
            <button class="icon-button icon-button--stack" :class="{ 'icon-button--active': rightTab === 'properties' }" type="button" title="属性" @click="rightTab = 'properties'; toggleInspectorPane()"><span>属</span></button>
            <button class="icon-button icon-button--stack" :class="{ 'icon-button--active': rightTab === 'ai' }" type="button" title="AI" @click="rightTab = 'ai'; toggleInspectorPane()"><span>AI</span></button>
          </section>
        </aside>
      </section>

      <section v-else-if="selectedView === 'graph'" class="graph-layout">
        <section class="panel-shell graph-panel">
          <header class="section-header">
            <div>
              <p class="eyebrow">Knowledge Graph</p>
              <h3>关系图谱</h3>
            </div>
            <div class="action-row action-row--tight">
              <span class="pill">{{ graphStats.nodes }} 笔记</span>
              <span class="pill">{{ graphStats.edges }} 链接</span>
              <span class="pill">{{ graphStats.tags }} 标签</span>
              <button class="icon-button" type="button" title="缩小图谱" @click="zoomOut"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 12h12" /></svg></button>
              <button class="icon-button" type="button" title="恢复图谱缩放" @click="resetZoom"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12a7 7 0 1 0 2-4.9" /><path d="M5 5v5h5" /></svg></button>
              <button class="icon-button" type="button" title="放大图谱" @click="zoomIn"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v12" /><path d="M6 12h12" /></svg></button>
            </div>
          </header>

          <div class="graph-stage" @wheel.prevent="handleGraphWheel">
            <svg class="graph-svg" viewBox="0 0 100 100" aria-label="knowledge graph">
              <g :transform="graphTransform">
                <line
                  v-for="edge in graphEdges"
                  :key="edge.key"
                  class="graph-edge"
                  :class="{ 'graph-edge--active': edge.emphasis }"
                  :x1="edge.x1"
                  :y1="edge.y1"
                  :x2="edge.x2"
                  :y2="edge.y2"
                />

                <g
                  v-for="node in graphNodes"
                  :key="node.slug"
                  class="graph-node"
                  :class="{ 'graph-node--focused': node.isFocused }"
                  @click="selectNote(node.slug)"
                >
                  <circle :cx="node.x" :cy="node.y" :r="node.size / 2" :fill="node.color" fill-opacity="0.14" />
                  <circle :cx="node.x" :cy="node.y" :r="Math.max(3.4, node.size / 4)" :fill="node.color" />
                  <text :x="node.x" :y="node.y + node.size / 2 + 3" text-anchor="middle">{{ node.title }}</text>
                </g>
              </g>
            </svg>
          </div>
        </section>

        <aside class="graph-sidebar">
          <section class="panel-shell panel-shell--compact">
            <p class="eyebrow">Focus</p>
            <h3>{{ currentNote?.title ?? '未选择笔记' }}</h3>
          </section>
          <section class="panel-shell panel-shell--compact">
            <p class="eyebrow">Dense Notes</p>
            <div class="result-list">
              <button v-for="note in denseNotes" :key="note.slug" class="result-card" type="button" @click="selectNote(note.slug)">
                <strong>{{ note.title }}</strong>
                <span>{{ note.path }}</span>
                <p>{{ note.degree }} 条关联，{{ note.tags.length }} 个标签</p>
              </button>
            </div>
          </section>
        </aside>
      </section>

      <section v-else-if="selectedView === 'ai'" class="ai-layout">
        <section class="panel-shell ai-panel">
          <header class="section-header">
            <div>
              <p class="eyebrow">AI Studio</p>
              <h3>创作中心</h3>
            </div>
            <span class="pill">上下文 {{ aiReferences.length }}</span>
          </header>

          <div class="ai-grid">
            <div class="panel-subtle">
              <span class="section-label">当前焦点笔记</span>
              <select class="field-input" :value="currentNote?.slug ?? ''" @change="selectNote(($event.target as HTMLSelectElement).value)">
                <option v-for="note in filteredNotes" :key="note.slug" :value="note.slug">{{ note.title }}</option>
              </select>
              <div class="action-row">
                <label class="toggle-chip">
                  <input v-model="useRemoteAi" type="checkbox" />
                  <span>创作时连接 AI</span>
                </label>
                <label class="toggle-chip">
                  <input v-model="useRag" type="checkbox" />
                  <span>带入 RAG 参考</span>
                </label>
                <button class="icon-button" type="button" title="打开设置" @click="openView('settings')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10.3 2.8h3.4l.5 2.2a7.6 7.6 0 0 1 1.8.8l2-1.1 2.4 2.4-1.1 2a7.6 7.6 0 0 1 .8 1.8l2.2.5v3.4l-2.2.5a7.6 7.6 0 0 1-.8 1.8l1.1 2-2.4 2.4-2-1.1a7.6 7.6 0 0 1-1.8.8l-.5 2.2h-3.4l-.5-2.2a7.6 7.6 0 0 1-1.8-.8l-2 1.1-2.4-2.4 1.1-2a7.6 7.6 0 0 1-.8-1.8l-2.2-.5v-3.4l2.2-.5a7.6 7.6 0 0 1 .8-1.8l-1.1-2 2.4-2.4 2 1.1a7.6 7.6 0 0 1 1.8-.8z" /><circle cx="12" cy="12" r="3.1" /></svg></button>
              </div>
              <textarea
                v-model="aiPrompt"
                class="field-input textarea-input textarea-input--large"
                placeholder="输入创作要求，例如生成文章框架、重写摘要、基于知识库起草内容"
              />
              <div class="action-row">
                <button class="icon-button" type="button" :disabled="isBusy" title="总结" @click="runAi('summary')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 7h12" /><path d="M6 12h8" /><path d="M6 17h10" /></svg></button>
                <button class="icon-button" type="button" :disabled="isBusy" title="扩写" @click="runAi('expand')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
                <button class="icon-button" type="button" :disabled="isBusy" title="润色" @click="runAi('polish')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 17.5V20h2.5L18 8.5 15.5 6z" /><path d="M13.5 8 16 10.5" /></svg></button>
                <button class="icon-button" type="button" :disabled="isBusy" title="问答" @click="runAi('qa')"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M9.5 9.8a2.5 2.5 0 0 1 5 0c0 1.6-2.5 2-2.5 4" /><path d="M12 17h.01" /></svg></button>
                <button class="icon-button icon-button--primary" type="button" :disabled="isBusy" :title="isBusy ? '处理中' : '创作并保存'" @click="runAi('create', true)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /></svg></button>
              </div>
            </div>

            <div class="panel-subtle">
              <span class="section-label">生成结果</span>
              <pre class="ai-output ai-output--large">{{ aiResponse || '结果会显示在这里。' }}</pre>
              <button class="icon-button icon-button--primary" type="button" :disabled="!showAiPreview || !aiResponse" title="追加到当前笔记" @click="applyAiResultToCurrentNote"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
            </div>
          </div>

          <div class="ai-upload-grid">
            <section class="panel-subtle">
              <span class="section-label">文档提炼</span>
              <input class="field-input" type="file" accept=".txt,.md,.docx,.pptx,.pdf" @change="uploadDocument" />
              <div v-if="uploadedDocument" class="document-preview">
                <strong>{{ uploadedDocument.extracted_title }}</strong>
                <p>{{ uploadedDocument.summary || uploadedDocument.content_preview }}</p>
              </div>
            </section>

            <section class="panel-subtle">
              <span class="section-label">参考片段</span>
              <div class="result-list">
                <button
                  v-for="item in aiReferences"
                  :key="`${item.note_slug}-${item.score}`"
                  class="result-card"
                  type="button"
                  @click="item.note_slug ? selectNote(item.note_slug) : undefined"
                >
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.score }}</span>
                  <p>{{ item.snippet }}</p>
                </button>
              </div>
            </section>
          </div>
        </section>
      </section>

      <section v-else class="settings-layout">
        <div class="settings-grid">
          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Desktop Sync</p>
                <h3>系统设置</h3>
              </div>
              <span class="pill">{{ isApplyingEvents ? '正在落盘' : '空闲' }}</span>
            </header>

            <label class="field-group">
              <span>本地副本目录</span>
              <div class="field-inline">
                <input v-model="desktopRootPath" class="field-input" placeholder="例如 D:/KnowledgeCloudVault" />
                <button class="icon-button" type="button" title="校验目录" @click="validateRootPath"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m20 6-11 11-5-5" /></svg></button>
              </div>
            </label>

            <label class="toggle-chip toggle-chip--block">
              <input v-model="autoSyncEnabled" type="checkbox" />
              <span>接收到同步事件后自动写入本地副本</span>
            </label>
            <p class="helper-text">{{ agentMessage }}</p>
            <p v-if="!hasTauriRuntime" class="helper-text helper-text--error">当前不是 Tauri 壳层，无法真正写入本地文件系统。</p>

            <div class="stats-grid">
              <article class="metric-card metric-card--plain">
                <span>待同步事件</span>
                <strong>{{ syncStatus.pending_events }}</strong>
              </article>
              <article class="metric-card metric-card--plain">
                <span>在线桌面设备</span>
                <strong>{{ syncStatus.connected_desktop_devices }}</strong>
              </article>
              <article class="metric-card metric-card--plain">
                <span>最新事件 ID</span>
                <strong>{{ syncStatus.last_event_id ?? 0 }}</strong>
              </article>
            </div>
          </section>

          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Instance</p>
                <h3>实例与部署</h3>
              </div>
              <button class="icon-button icon-button--primary" type="button" :disabled="!isInstanceAdmin" title="保存实例" @click="saveInstanceConfig"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4.5h12l2 2V19H5z" /><path d="M8 4.5V9h8V4.5" /><path d="M9 19v-6h6v6" /></svg></button>
            </header>

            <div class="field-grid">
              <label class="field-group field-group--full">
                <span>实例名称</span>
                <input v-model="instanceBootstrapForm.instance_name" class="field-input" placeholder="例如 个人双向知识库" :disabled="!isInstanceAdmin" />
              </label>
              <label class="field-group">
                <span>部署方式</span>
                <select v-model="instanceBootstrapForm.deployment_mode" class="field-input" :disabled="!isInstanceAdmin">
                  <option value="desktop">主电脑部署</option>
                  <option value="server">服务器部署</option>
                </select>
              </label>
              <label class="field-group">
                <span>版本类型</span>
                <select v-model="instanceBootstrapForm.edition" class="field-input" :disabled="!isInstanceAdmin">
                  <option value="personal">个人版</option>
                  <option value="team">团队版</option>
                </select>
              </label>
              <label v-if="instanceBootstrapForm.edition === 'team'" class="field-group field-group--full">
                <span>团队授权码</span>
                <input v-model="instanceBootstrapForm.authorization_code" class="field-input" type="password" placeholder="输入团队授权码" :disabled="!isInstanceAdmin" />
              </label>
            </div>

            <div class="stats-grid">
              <article class="metric-card metric-card--plain">
                <span>实例 ID</span>
                <strong>{{ instanceConfig?.instance_id?.slice(0, 12) || '未初始化' }}</strong>
              </article>
              <article class="metric-card metric-card--plain">
                <span>当前模式</span>
                <strong>{{ instanceConfig?.deployment_mode === 'server' ? '服务器' : '主电脑' }}</strong>
              </article>
              <article class="metric-card metric-card--plain">
                <span>授权状态</span>
                <strong>{{ instanceConfig?.team_license_verified ? '已授权' : '未授权' }}</strong>
              </article>
            </div>

            <p class="helper-text">
              {{ instanceConfig?.edition === 'team' ? '团队版支持成员共享、专属空间和团队 RAG。' : '个人版默认单用户隔离，可自由安装部署。' }}
            </p>
            <p v-if="!isInstanceAdmin" class="helper-text helper-text--error">只有管理员可以修改实例配置。</p>
          </section>

          <section class="panel-shell">
            <header class="section-header">
              <div>
                <p class="eyebrow">Model Service</p>
                <h3>模型配置</h3>
              </div>
              <button class="icon-button icon-button--primary" type="button" title="保存模型配置" @click="saveProviderConfig"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4.5h12l2 2V19H5z" /><path d="M8 4.5V9h8V4.5" /><path d="M9 19v-6h6v6" /></svg></button>
            </header>

            <div class="field-grid">
              <label class="field-group">
                <span>服务商</span>
                <select v-model="providerForm.provider_id" class="field-input" @change="applyProviderPreset(providerForm.provider_id)">
                  <option v-for="preset in providerPresets" :key="preset.provider_id" :value="preset.provider_id">{{ preset.label }}</option>
                </select>
              </label>
              <label class="field-group">
                <span>展示名称</span>
                <input v-model="providerForm.provider_label" class="field-input" placeholder="例如 OpenAI" />
              </label>
              <label class="field-group field-group--full">
                <span>Base URL</span>
                <input v-model="providerForm.base_url" class="field-input" placeholder="https://api.openai.com/v1" />
              </label>
              <label class="field-group">
                <span>模型名</span>
                <input v-model="providerForm.model_name" class="field-input" list="desktop-models" placeholder="模型名" />
                <datalist id="desktop-models">
                  <option v-for="model in currentPresetModels" :key="model" :value="model" />
                </datalist>
              </label>
              <label class="field-group">
                <span>新的 API Key</span>
                <input v-model="providerForm.api_key" class="field-input" type="password" placeholder="仅在需要变更时填写" />
              </label>
            </div>

            <div class="action-row">
              <label class="toggle-chip">
                <input v-model="providerForm.is_enabled" type="checkbox" />
                <span>启用远端模型</span>
              </label>
              <label class="toggle-chip">
                <input v-model="useRemoteAi" type="checkbox" />
                <span>创作时连接 AI</span>
              </label>
              <label class="toggle-chip">
                <input v-model="useRag" type="checkbox" />
                <span>带入 RAG 参考</span>
              </label>
            </div>
            <p class="helper-text">当前模型：{{ providerCurrent?.provider_label || '未配置' }} {{ providerCurrent?.api_key_masked || '' }}</p>
          </section>
        </div>

        <section class="panel-shell">
          <header class="section-header">
            <div>
              <p class="eyebrow">Sync Logs</p>
              <h3>同步日志</h3>
            </div>
            <div class="action-row">
              <button class="icon-button" type="button" title="全量刷新" @click="refreshEvents(true)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12a7 7 0 1 0 2-4.9" /><path d="M5 5v5h5" /></svg></button>
              <button class="icon-button" type="button" title="重试失败" @click="retryFailedEvents"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m20 6-11 11-5-5" /><path d="M15 6h5v5" /></svg></button>
              <button class="icon-button icon-button--primary" type="button" title="确认可见事件" @click="ackVisibleEvents"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m20 6-11 11-5-5" /></svg></button>
            </div>
          </header>

          <div class="event-list">
            <article
              v-for="event in events"
              :key="event.id"
              class="event-card"
              :class="[`event-card--${event.status}`]"
            >
              <div class="event-card__top">
                <strong>#{{ event.id }} {{ event.event_type }}</strong>
                <span>{{ event.status }}</span>
              </div>
              <span>{{ event.path || '无路径' }}</span>
              <span v-if="event.target_path">→ {{ event.target_path }}</span>
              <small>{{ event.created_at }}</small>
              <small v-if="event.message" class="helper-text helper-text--error">{{ event.message }}</small>
            </article>
          </div>
        </section>
      </section>
    </section>

    <div v-if="showLoginModal" class="modal-backdrop" @click.self="closeLoginModal">
      <div class="modal-card modal-card--auth">
        <div class="modal-card__header">
          <div>
            <h3>{{ loginModalTitle }}</h3>
            <p class="helper-text">{{ loginModalDescription }}</p>
          </div>
          <button v-if="isConnected" class="icon-button" type="button" title="关闭账户面板" @click="closeLoginModal"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12" /><path d="M18 6 6 18" /></svg></button>
        </div>

        <div v-if="currentUser" class="account-summary">
          <div class="account-summary__avatar">{{ displayUserInitial }}</div>
          <div>
            <strong>{{ displayUserName }}</strong>
            <p class="helper-text">{{ displayUserMeta }} · {{ currentUser.edition === 'team' ? '团队版' : '个人版' }}</p>
          </div>
        </div>

        <form class="auth-card auth-card--modal" @submit.prevent="login">
          <label class="field-group">
            <span>账号</span>
            <input v-model="username" class="field-input" placeholder="账号" />
          </label>
          <label class="field-group">
            <span>密码</span>
            <input v-model="password" class="field-input" type="password" placeholder="密码" />
          </label>
          <p v-if="errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
          <div class="modal-actions modal-actions--split">
            <button v-if="isConnected" class="icon-button icon-button--danger" type="button" title="退出当前账户" @click="logout">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 7V5.5A1.5 1.5 0 0 0 12.5 4h-6A1.5 1.5 0 0 0 5 5.5v13A1.5 1.5 0 0 0 6.5 20h6A1.5 1.5 0 0 0 14 18.5V17" /><path d="M10 12h10" /><path d="m17 8 3 4-3 4" /></svg>
            </button>
            <button class="icon-button icon-button--primary" type="submit" :disabled="isBusy" :title="isBusy ? '连接中' : '登录'">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></svg>
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showClipPreview && clipPreview" class="modal-backdrop" @click.self="closeClipPreview">
      <div class="modal-card">
        <div class="modal-card__header">
          <div>
            <h3>{{ clipSummarizeWithAi ? '提炼预览' : '解析预览' }}</h3>
            <p class="helper-text">{{ clipPreview.extracted_title }}</p>
          </div>
          <button class="icon-button" type="button" title="关闭预览" @click="closeClipPreview"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12" /><path d="M18 6 6 18" /></svg></button>
        </div>
        <pre class="ai-output ai-output--large">{{ clipPreview.preview_content }}</pre>
        <div class="modal-actions">
          <button class="icon-button" type="button" title="取消" @click="closeClipPreview"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12" /><path d="M18 6 6 18" /></svg></button>
          <button class="icon-button icon-button--primary" type="button" :disabled="isBusy" title="同意并保存" @click="saveClipPreview"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m20 6-11 11-5-5" /></svg></button>
        </div>
      </div>
    </div>
  </main>
</template>
