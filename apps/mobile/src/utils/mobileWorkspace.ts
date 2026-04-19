import { reactive } from 'vue'

import { clearToken, request, setToken, uploadFile } from './api'

export type NoteRecord = {
  id: number
  title: string
  slug: string
  content: string
  path: string
  parent_path?: string
  tags: string[]
  links: string[]
  summary: string
  version?: string
  updated_at?: string
}

export type TreeNode = {
  name: string
  path: string
  node_type: 'folder' | 'file'
  note_slug?: string | null
  children: TreeNode[]
}

type BacklinkBundle = {
  outgoing: Array<{ target_slug: string }>
  incoming: Array<{ source_slug: string }>
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
}

type ClipPreviewResult = {
  note?: NoteRecord | null
  extracted_title: string
  source_url: string
  summary: string
  preview_content: string
  saved: boolean
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

type KnowledgeSpace = {
  id: number
  instance_id: string
  owner_user_id: number
  slug: string
  name: string
  visibility: 'private' | 'team'
  is_default: boolean
}

type TeamMember = {
  user_id: number
  username: string
  display_name: string
  role: string
  default_space_slug: string
  content_visibility_scope: string
}

type TeamInvite = {
  invite_code: string
  role: string
  status: string
  expires_at?: string | null
  invite_link: string
}

export const mobileWorkspace = reactive({
  username: 'admin',
  password: 'admin123456',
  isAuthenticated: false,
  isLoading: false,
  errorMessage: '',
  fileTree: [] as TreeNode[],
  currentUser: null as CurrentUser | null,
  instanceConfig: null as InstanceConfig | null,
  instanceBootstrapForm: {
    instance_name: '个人双向知识库',
    deployment_mode: 'desktop' as 'desktop' | 'server',
    edition: 'personal' as 'personal' | 'team',
    authorization_code: '',
  },
  instanceBootstrapStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
  spaces: [] as KnowledgeSpace[],
  currentSpaceSlug: '',
  teamMembers: [] as TeamMember[],
  latestInvite: null as TeamInvite | null,
  teamInviteStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
  teamInviteForm: {
    role: 'member' as 'member' | 'manager',
    expires_in_hours: 72,
  },
  teamInviteAcceptStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
  teamInviteAcceptForm: {
    invite_code: '',
    username: '',
    password: '',
    display_name: '',
  },
  createSpaceForm: {
    name: '',
    slug: '',
    visibility: 'private' as 'private' | 'team',
  },
  createSpaceStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
  notes: [] as NoteRecord[],
  currentNote: null as NoteRecord | null,
  outgoingLinks: [] as string[],
  incomingLinks: [] as string[],
  tags: [] as TagSummary[],
  searchQuery: '',
  searchResults: [] as SearchResult[],
  syncStatus: {
    pending_events: 0,
    connected_desktop_devices: 0,
  } as SyncStatus,
  clipUrl: '',
  clipSummarizeWithAi: true,
  clipSummaryPrompt: '请提炼文章的关键信息、结构脉络和可执行建议。',
  clipPreview: null as ClipPreviewResult | null,
  clipPreviewOpen: false,
  aiPrompt: '',
  aiResult: '',
  aiReferences: [] as Array<{ title: string; score: string; snippet: string; note_slug?: string }>,
  useRemoteAi: true,
  useRag: true,
  uploadedDocumentSummary: '',
  aiProviderPresets: [] as AiProviderPreset[],
  aiProviderCurrent: null as AiProviderConfig | null,
  aiProviderForm: {
    provider_id: 'custom',
    provider_label: '自定义 OpenAI 兼容',
    base_url: 'https://api.openai.com/v1',
    model_name: 'gpt-5',
    api_key: '',
    is_enabled: true,
  },
  aiProviderSaveStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
})

let bootstrapPromise: Promise<void> | null = null

function setError(message: string) {
  mobileWorkspace.errorMessage = message
  if (message) {
    uni.showToast({ title: message, icon: 'none', duration: 2200 })
  }
}

function resolveCurrentSpaceSlug() {
  return mobileWorkspace.currentSpaceSlug || mobileWorkspace.currentUser?.default_space_slug || undefined
}

function syncCurrentSpace(spaces: KnowledgeSpace[]) {
  const preferredSpaceSlug = mobileWorkspace.currentSpaceSlug || mobileWorkspace.currentUser?.default_space_slug || spaces[0]?.slug || ''
  mobileWorkspace.currentSpaceSlug = spaces.some((space) => space.slug === preferredSpaceSlug)
    ? preferredSpaceSlug
    : (spaces[0]?.slug || '')
}

export function setCurrentSpace(spaceSlug: string) {
  if (mobileWorkspace.spaces.some((space) => space.slug === spaceSlug)) {
    mobileWorkspace.currentSpaceSlug = spaceSlug
  }
}

export async function bootstrapMobileWorkspace(force = false) {
  if (!force && bootstrapPromise) {
    return bootstrapPromise
  }

  bootstrapPromise = (async () => {
    mobileWorkspace.isLoading = true
    mobileWorkspace.errorMessage = ''
    try {
      await loadInstanceConfig()
      mobileWorkspace.currentUser = await request<CurrentUser>('/auth/me')
      mobileWorkspace.isAuthenticated = true
      await loadWorkspace()
      return
    } catch {
      clearToken()
      mobileWorkspace.isAuthenticated = false
      mobileWorkspace.currentUser = null
    }

    try {
      await login(true)
    } catch {
      setError('移动端连接云端失败，请先确认后端服务可用。')
    } finally {
      mobileWorkspace.isLoading = false
    }
  })()

  try {
    await bootstrapPromise
  } finally {
    bootstrapPromise = null
    mobileWorkspace.isLoading = false
  }
}

export async function login(silent = false) {
  mobileWorkspace.isLoading = true
  mobileWorkspace.errorMessage = ''
  try {
    const result = await request<{ access_token: string }>('/auth/login', 'POST', {
      username: mobileWorkspace.username,
      password: mobileWorkspace.password,
    })
    setToken(result.access_token)
    mobileWorkspace.currentUser = await request<CurrentUser>('/auth/me')
    mobileWorkspace.isAuthenticated = true
    await loadWorkspace()
    if (!silent) {
      uni.showToast({ title: '已连接云端', icon: 'success' })
    }
  } catch (error) {
    mobileWorkspace.isAuthenticated = false
    if (!silent) {
      setError('登录失败，请检查账号或服务状态。')
    }
    throw error
  } finally {
    mobileWorkspace.isLoading = false
  }
}

export async function loadInstanceConfig() {
  const result = await request<InstanceConfig>('/instance/config')
  mobileWorkspace.instanceConfig = result
  mobileWorkspace.instanceBootstrapForm.instance_name = result.instance_name
  mobileWorkspace.instanceBootstrapForm.deployment_mode = result.deployment_mode
  mobileWorkspace.instanceBootstrapForm.edition = result.edition
}

export async function loadWorkspace() {
  const [tree, notes, tags, syncStatus, instanceConfig, spaces] = await Promise.all([
    request<TreeNode[]>('/tree'),
    request<NoteRecord[]>('/notes'),
    request<TagSummary[]>('/tags'),
    request<SyncStatus>('/sync/status'),
    request<InstanceConfig>('/instance/config'),
    request<KnowledgeSpace[]>('/instance/spaces'),
  ])
  mobileWorkspace.fileTree = tree
  mobileWorkspace.notes = notes
  mobileWorkspace.tags = tags
  mobileWorkspace.syncStatus = syncStatus
  mobileWorkspace.instanceConfig = instanceConfig
  mobileWorkspace.spaces = spaces
  mobileWorkspace.instanceBootstrapForm.instance_name = instanceConfig.instance_name
  mobileWorkspace.instanceBootstrapForm.deployment_mode = instanceConfig.deployment_mode
  mobileWorkspace.instanceBootstrapForm.edition = instanceConfig.edition
  syncCurrentSpace(spaces)
  await loadAiProviderCatalog()
  if (mobileWorkspace.currentUser?.role === 'admin' && instanceConfig.edition === 'team') {
    await loadTeamMembers()
  }
  if (notes.length > 0 && !mobileWorkspace.currentNote) {
    await selectNote(notes[0].slug)
  }
}

export async function selectNote(slug: string) {
  mobileWorkspace.currentNote = await request<NoteRecord>(`/notes/${slug}`)
  const links = await request<BacklinkBundle>(`/links/${slug}`)
  mobileWorkspace.outgoingLinks = links.outgoing.map((item) => item.target_slug)
  mobileWorkspace.incomingLinks = links.incoming.map((item) => item.source_slug)
}

export async function createQuickNote(pathPrefix = '01_Notes') {
  const now = new Date().toISOString().slice(0, 16).replace('T', ' ')
  const title = `新笔记 ${now}`
  const slug = title.toLowerCase().replace(/[^\w\u4e00-\u9fff-]+/g, '-')
  const note = await request<NoteRecord>('/notes', 'POST', {
    title,
    slug,
    content: `# ${title}\n\n在这里输入内容。`,
    path: `${pathPrefix}/${title}.md`,
    space_slug: resolveCurrentSpaceSlug(),
    tags: [],
    links: [],
    source_url: '',
  })
  await loadWorkspace()
  await selectNote(note.slug)
  return note.slug
}

export async function createFolder(path: string) {
  await request('/folders', 'POST', { path })
  await loadWorkspace()
}

export async function searchNotes() {
  if (mobileWorkspace.searchQuery.trim().length < 2) {
    mobileWorkspace.searchResults = []
    return
  }
  const result = await request<{ results: SearchResult[] }>(`/search?q=${encodeURIComponent(mobileWorkspace.searchQuery)}`)
  mobileWorkspace.searchResults = result.results
}

export async function submitClipPreview() {
  if (!mobileWorkspace.clipUrl.trim()) {
    setError('请先输入文章链接。')
    return
  }
  mobileWorkspace.isLoading = true
  mobileWorkspace.errorMessage = ''
  try {
    mobileWorkspace.clipPreview = await request<ClipPreviewResult>('/clip', 'POST', {
      url: mobileWorkspace.clipUrl,
      device_id: 'mobile-app',
      summarize_with_ai: mobileWorkspace.clipSummarizeWithAi,
      use_ai: mobileWorkspace.useRemoteAi,
      use_rag: mobileWorkspace.useRag,
      summary_prompt: mobileWorkspace.clipSummaryPrompt,
      target_folder: '00_Inbox',
      space_slug: resolveCurrentSpaceSlug(),
      save_to_note: false,
    })
    mobileWorkspace.clipPreviewOpen = true
  } catch (error: any) {
    setError(error?.data?.detail || error?.response?.data?.detail || '解析失败，请稍后重试。')
  } finally {
    mobileWorkspace.isLoading = false
  }
}

export async function saveClipPreview() {
  if (!mobileWorkspace.clipPreview) {
    return
  }
  mobileWorkspace.isLoading = true
  try {
    const result = await request<ClipPreviewResult>('/clip/save-preview', 'POST', {
      title: mobileWorkspace.clipPreview.extracted_title,
      source_url: mobileWorkspace.clipPreview.source_url,
      content: mobileWorkspace.clipPreview.preview_content,
      summary: mobileWorkspace.clipPreview.summary,
      target_folder: '00_Inbox',
      space_slug: resolveCurrentSpaceSlug(),
      device_id: 'mobile-app',
    })
    mobileWorkspace.clipPreviewOpen = false
    mobileWorkspace.clipPreview = null
    mobileWorkspace.clipUrl = ''
    if (result.note?.slug) {
      await loadWorkspace()
      await selectNote(result.note.slug)
    }
    uni.showToast({ title: '已保存到知识库', icon: 'success' })
  } catch {
    setError('保存剪藏失败，请稍后重试。')
  } finally {
    mobileWorkspace.isLoading = false
  }
}

export function closeClipPreview() {
  mobileWorkspace.clipPreviewOpen = false
  mobileWorkspace.clipPreview = null
}

export async function runAi(action: 'summary' | 'expand' | 'polish' | 'qa' | 'create', saveAsNote = false) {
  mobileWorkspace.isLoading = true
  mobileWorkspace.errorMessage = ''
  try {
    const result = await request<{ content: string; references: Array<{ title: string; score: string; snippet: string; note_slug?: string }>; saved_note?: NoteRecord | null }>(
      '/ai/generate',
      'POST',
      {
        action,
        prompt: mobileWorkspace.aiPrompt || '请基于当前笔记和知识库给出结果',
        note_slug: mobileWorkspace.currentNote?.slug,
        save_as_note: saveAsNote,
        target_folder: '00_Inbox',
        space_slug: resolveCurrentSpaceSlug(),
        use_ai: mobileWorkspace.useRemoteAi,
        use_rag: mobileWorkspace.useRag,
      },
    )
    mobileWorkspace.aiResult = result.content
    mobileWorkspace.aiReferences = result.references || []
    if (result.saved_note?.slug) {
      await loadWorkspace()
      await selectNote(result.saved_note.slug)
    }
  } catch (error: any) {
    setError(error?.data?.detail || error?.response?.data?.detail || '创作失败，请稍后重试。')
  } finally {
    mobileWorkspace.isLoading = false
  }
}

export async function uploadDocument() {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    success: async (result) => {
      const file = result.tempFiles?.[0]
      if (!file?.path) {
        return
      }
      mobileWorkspace.isLoading = true
      try {
        const response = await uploadFile<{ extracted_title: string; summary: string; saved_note?: NoteRecord | null }>('/documents/upload', file.path, {
          summarize_with_ai: 'true',
          use_ai: String(mobileWorkspace.useRemoteAi),
          use_rag: String(mobileWorkspace.useRag),
          save_to_note: 'true',
          target_folder: '03_Resources',
          space_slug: resolveCurrentSpaceSlug() || '',
        })
        mobileWorkspace.uploadedDocumentSummary = `${response.extracted_title} 已提炼并存入知识库`
        await loadWorkspace()
      } catch {
        setError('文档上传失败，请检查文件或服务状态。')
      } finally {
        mobileWorkspace.isLoading = false
      }
    },
  })
}

export async function loadAiProviderCatalog() {
  const result = await request<{ presets: AiProviderPreset[]; current: AiProviderConfig | null }>('/ai/providers/catalog')
  mobileWorkspace.aiProviderPresets = result.presets
  mobileWorkspace.aiProviderCurrent = result.current
  if (result.current) {
    mobileWorkspace.aiProviderForm.provider_id = result.current.provider_id
    mobileWorkspace.aiProviderForm.provider_label = result.current.provider_label
    mobileWorkspace.aiProviderForm.base_url = result.current.base_url
    mobileWorkspace.aiProviderForm.model_name = result.current.model_name
    mobileWorkspace.aiProviderForm.is_enabled = result.current.is_enabled
  } else if (result.presets[0]) {
    applyProviderPreset(result.presets[0].provider_id)
  }
}

export function applyProviderPreset(providerId: string) {
  const preset = mobileWorkspace.aiProviderPresets.find((item) => item.provider_id === providerId)
  if (!preset) {
    return
  }
  mobileWorkspace.aiProviderForm.provider_id = preset.provider_id
  mobileWorkspace.aiProviderForm.provider_label = preset.label
  mobileWorkspace.aiProviderForm.base_url = preset.base_url
  mobileWorkspace.aiProviderForm.model_name = preset.models[0] || ''
}

export async function saveAiProviderConfig() {
  mobileWorkspace.aiProviderSaveStatus = 'saving'
  try {
    const result = await request<AiProviderConfig>('/ai/providers/current', 'PUT', {
      provider_id: mobileWorkspace.aiProviderForm.provider_id,
      provider_label: mobileWorkspace.aiProviderForm.provider_label,
      base_url: mobileWorkspace.aiProviderForm.base_url,
      api_key: mobileWorkspace.aiProviderForm.api_key,
      model_name: mobileWorkspace.aiProviderForm.model_name,
      is_enabled: mobileWorkspace.aiProviderForm.is_enabled,
    })
    mobileWorkspace.aiProviderCurrent = result
    mobileWorkspace.aiProviderForm.api_key = ''
    mobileWorkspace.aiProviderSaveStatus = 'saved'
    uni.showToast({ title: '模型配置已保存', icon: 'success' })
  } catch (error: any) {
    mobileWorkspace.aiProviderSaveStatus = 'error'
    setError(error?.data?.detail || error?.response?.data?.detail || '模型配置保存失败。')
  }
}

export async function bootstrapInstanceConfig() {
  mobileWorkspace.instanceBootstrapStatus = 'saving'
  try {
    const result = await request<InstanceConfig>('/instance/bootstrap', 'PUT', mobileWorkspace.instanceBootstrapForm)
    mobileWorkspace.instanceConfig = result
    mobileWorkspace.instanceBootstrapStatus = 'saved'
    await loadWorkspace()
  } catch (error: any) {
    mobileWorkspace.instanceBootstrapStatus = 'error'
    setError(error?.data?.detail || error?.response?.data?.detail || '实例配置保存失败。')
  }
}

export async function loadSpaces() {
  mobileWorkspace.spaces = await request<KnowledgeSpace[]>('/instance/spaces')
  syncCurrentSpace(mobileWorkspace.spaces)
}

export async function createSpace() {
  mobileWorkspace.createSpaceStatus = 'saving'
  try {
    await request('/instance/spaces', 'POST', mobileWorkspace.createSpaceForm)
    mobileWorkspace.createSpaceStatus = 'saved'
    mobileWorkspace.createSpaceForm = {
      name: '',
      slug: '',
      visibility: 'private',
    }
    await loadSpaces()
  } catch (error: any) {
    mobileWorkspace.createSpaceStatus = 'error'
    setError(error?.data?.detail || error?.response?.data?.detail || '创建空间失败。')
  }
}

export async function loadTeamMembers() {
  if (mobileWorkspace.instanceConfig?.edition !== 'team' || mobileWorkspace.currentUser?.role !== 'admin') {
    mobileWorkspace.teamMembers = []
    return
  }
  mobileWorkspace.teamMembers = await request<TeamMember[]>('/instance/team/members')
}

export async function createTeamInvite() {
  mobileWorkspace.teamInviteStatus = 'saving'
  try {
    mobileWorkspace.latestInvite = await request<TeamInvite>('/instance/team/invites', 'POST', mobileWorkspace.teamInviteForm)
    mobileWorkspace.teamInviteStatus = 'saved'
    await loadTeamMembers()
  } catch (error: any) {
    mobileWorkspace.teamInviteStatus = 'error'
    setError(error?.data?.detail || error?.response?.data?.detail || '生成邀请失败。')
  }
}

export async function acceptTeamInvite() {
  mobileWorkspace.teamInviteAcceptStatus = 'saving'
  try {
    await request('/instance/team/invites/accept', 'POST', mobileWorkspace.teamInviteAcceptForm)
    mobileWorkspace.teamInviteAcceptStatus = 'saved'
    mobileWorkspace.username = mobileWorkspace.teamInviteAcceptForm.username
    mobileWorkspace.password = mobileWorkspace.teamInviteAcceptForm.password
    await login(true)
    mobileWorkspace.teamInviteAcceptForm = {
      invite_code: '',
      username: '',
      password: '',
      display_name: '',
    }
    uni.showToast({ title: '已加入团队', icon: 'success' })
  } catch (error: any) {
    mobileWorkspace.teamInviteAcceptStatus = 'error'
    setError(error?.data?.detail || error?.response?.data?.detail || '接受邀请失败。')
  }
}