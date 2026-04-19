import { defineStore } from 'pinia'

import { apiClient, setAccessToken } from '../api/client'

export type TreeNode = {
  name: string
  path: string
  node_type: 'folder' | 'file'
  note_slug?: string | null
  children: TreeNode[]
}

export type NoteRecord = {
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

type SyncStatus = {
  pending_events: number
  connected_desktop_devices: number
  last_event_id: number | null
}

type SearchResult = {
  kind: 'note' | 'password'
  title: string
  path: string
  snippet: string
  note_slug?: string
  password_id?: number
}

type PasswordVaultConfig = {
  is_initialized: boolean
}

type PasswordEntry = {
  id: number
  title: string
  username: string
  category: string
  url: string
  website: string
  notes: string
  linked_note_slug: string
  vault_scope: 'private' | 'team' | 'selected'
  shared_member_ids: number[]
  editor_member_ids: number[]
  created_at: string
  updated_at: string
  last_used_at?: string | null
}

type PasswordAudit = {
  id: number
  entry_id: number
  actor_user_id: number
  action: string
  detail: string
  created_at: string
}

type GoalRecord = {
  id: number
  title: string
  vision: string
  key_results: string[]
  priority: 'low' | 'medium' | 'high'
  status: 'active' | 'paused' | 'completed'
  visibility_scope: 'private' | 'team'
  cycle_start?: string | null
  cycle_end?: string | null
  progress_percent: number
  created_at: string
  updated_at: string
}

type GoalPlan = {
  id: number
  goal_id: number
  title: string
  summary: string
  priority: 'low' | 'medium' | 'high'
  status: string
  sequence: number
  start_date?: string | null
  end_date?: string | null
  progress_percent: number
  created_at: string
  updated_at: string
}

type GoalTask = {
  id: number
  goal_id: number
  plan_id: number
  title: string
  details: string
  priority: 'low' | 'medium' | 'high'
  status: 'todo' | 'doing' | 'done' | 'cancelled'
  due_date?: string | null
  assignee_user_id?: number | null
  completed_at?: string | null
  created_at: string
  updated_at: string
}

type GoalJournal = {
  id: number
  goal_id: number
  plan_id: number
  journal_date: string
  note_slug: string
  task_ids: number[]
  reflection: string
  ai_summary: string
  updated_at: string
}

type GoalOverview = {
  goals: GoalRecord[]
  plans: GoalPlan[]
  today_tasks: GoalTask[]
  journals: GoalJournal[]
  today_note_slug: string
}

type GoalPlanSuggestion = {
  title: string
  summary: string
  tasks: string[]
}

type SystemPluginConfig = {
  plugin_id: 'topics' | 'ideas'
  is_enabled: boolean
}

type TopicRecord = {
  id: number
  title: string
  domain: string
  keywords: string[]
  source_type: 'manual' | 'ai' | 'team'
  status: 'writable' | 'pending' | 'in_progress' | 'completed' | 'shelved'
  priority: 'low' | 'medium' | 'high'
  heat_score: number
  trend_summary: string
  notes: string
  ai_outline: string
  linked_note_slug: string
  linked_password_entry_ids: number[]
  completed_note_slug: string
  assignee_user_id?: number | null
  review_status: 'none' | 'pending' | 'approved' | 'needs_revision'
  due_date?: string | null
  last_collected_at?: string | null
  created_at: string
  updated_at: string
}

type TopicActivityLog = {
  id: number
  topic_id: number
  actor_user_id: number
  action: string
  detail: string
  created_at: string
}

type TopicStats = {
  total: number
  completed: number
  writable: number
  pending: number
  in_progress: number
  shelved: number
  high_priority: number
  completion_rate: number
}

type TopicOverview = {
  plugin: SystemPluginConfig
  topics: TopicRecord[]
  stats: TopicStats
  recent_logs: TopicActivityLog[]
}

type IdeaRecord = {
  id: number
  title: string
  summary: string
  details: string
  idea_type: 'creative_idea' | 'user_need' | 'product_opportunity' | 'optimization'
  tags: string[]
  status: 'pending_review' | 'accepted' | 'planning' | 'building' | 'launched' | 'shelved'
  priority: 'low' | 'medium' | 'high'
  value_score: number
  effort_score: number
  business_score: number
  opportunity_score: number
  linked_note_slug: string
  linked_goal_id?: number | null
  linked_topic_id?: number | null
  source_context: string
  visibility_scope: 'private' | 'team'
  assignee_user_id?: number | null
  next_step: string
  created_at: string
  updated_at: string
}

type IdeaActivityLog = {
  id: number
  idea_id: number
  actor_user_id: number
  action: string
  detail: string
  created_at: string
}

type IdeaStats = {
  total: number
  pending_review: number
  accepted: number
  planning: number
  building: number
  launched: number
  shelved: number
  high_priority: number
  average_opportunity_score: number
}

type IdeaOverview = {
  plugin: SystemPluginConfig
  ideas: IdeaRecord[]
  stats: IdeaStats
  recent_logs: IdeaActivityLog[]
}

type TagSummary = {
  tag: string
  count: number
}

type AiProviderPreset = {
  provider_id: string
  label: string
  base_url: string
  models: string[]
}

type AiProviderConfig = {
  id: number
  provider_id: string
  provider_label: string
  base_url: string
  model_name: string
  api_key_masked: string
  has_api_key: boolean
  is_enabled: boolean
  is_default: boolean
}

type DocumentUploadResult = {
  filename: string
  extracted_title: string
  content_preview: string
  summary: string
  saved_note?: NoteRecord | null
}

type ClipPreviewResult = {
  note?: NoteRecord | null
  extracted_title: string
  source_url: string
  summary: string
  preview_content: string
  saved: boolean
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

type NoteSharing = {
  note_slug: string
  owner_user_id: number
  space_slug: string
  visibility_scope: 'private' | 'team' | 'selected'
  shared_member_ids: number[]
}

type SaveState = 'idle' | 'dirty' | 'saving' | 'saved' | 'error'

const tokenStorageKey = 'knowledge-cloud-token'
const recentVisitedStorageKey = 'knowledge-cloud-recent-visited'

export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({
    token: localStorage.getItem(tokenStorageKey) ?? '',
    username: 'admin',
    password: 'admin123456',
    isAuthenticated: false,
    isBusy: false,
    errorMessage: '',
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
    createSpaceStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    createSpaceForm: {
      name: '',
      slug: '',
      visibility: 'private' as 'private' | 'team',
    },
    fileTree: [] as TreeNode[],
    notes: [] as NoteRecord[],
    currentNote: null as NoteRecord | null,
    noteSharing: null as NoteSharing | null,
    noteSharingStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    backlinks: {
      note_slug: '',
      incoming: [] as LinkRecord[],
      outgoing: [] as LinkRecord[],
    },
    tags: [] as TagSummary[],
    searchQuery: '',
    searchResults: [] as SearchResult[],
    passwordVaultConfig: { is_initialized: false } as PasswordVaultConfig,
    passwordEntries: [] as PasswordEntry[],
    passwordAudits: [] as PasswordAudit[],
    selectedPasswordEntryId: 0,
    revealedPassword: '',
    passwordVerifyStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    passwordSetupStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    passwordSaveStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    passwordForm: {
      title: '',
      username: '',
      password: '',
      master_password: '',
      category: 'general',
      url: '',
      website: '',
      notes: '',
      linked_note_slug: '',
      vault_scope: 'private' as 'private' | 'team' | 'selected',
      shared_member_ids: [] as number[],
      editor_member_ids: [] as number[],
    },
    passwordGeneratorForm: {
      length: 20,
      include_symbols: true,
      include_numbers: true,
      include_uppercase: true,
    },
    goalOverview: {
      goals: [],
      plans: [],
      today_tasks: [],
      journals: [],
      today_note_slug: '',
    } as GoalOverview,
    goalSaveStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    goalTaskStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    goalAiPlanStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    goalForm: {
      title: '',
      vision: '',
      key_results: '' as string,
      priority: 'medium' as 'low' | 'medium' | 'high',
      visibility_scope: 'private' as 'private' | 'team',
      cycle_start: '',
      cycle_end: '',
    },
    goalPlanForm: {
      goal_id: 0,
      title: '',
      summary: '',
      priority: 'medium' as 'low' | 'medium' | 'high',
      start_date: '',
      end_date: '',
    },
    goalTaskForm: {
      goal_id: 0,
      plan_id: 0,
      title: '',
      details: '',
      priority: 'medium' as 'low' | 'medium' | 'high',
      due_date: '',
      assignee_user_id: null as number | null,
    },
    goalJournalForm: {
      journal_date: new Date().toISOString().slice(0, 10),
      goal_id: 0,
      plan_id: 0,
      reflection: '',
      task_ids: [] as number[],
    },
    goalAiPlanDraft: [] as GoalPlanSuggestion[],
    pluginConfigs: [] as SystemPluginConfig[],
    topicOverview: {
      plugin: { plugin_id: 'topics', is_enabled: false },
      topics: [],
      stats: {
        total: 0,
        completed: 0,
        writable: 0,
        pending: 0,
        in_progress: 0,
        shelved: 0,
        high_priority: 0,
        completion_rate: 0,
      },
      recent_logs: [],
    } as TopicOverview,
    ideaOverview: {
      plugin: { plugin_id: 'ideas', is_enabled: false },
      ideas: [],
      stats: {
        total: 0,
        pending_review: 0,
        accepted: 0,
        planning: 0,
        building: 0,
        launched: 0,
        shelved: 0,
        high_priority: 0,
        average_opportunity_score: 0,
      },
      recent_logs: [],
    } as IdeaOverview,
    topicSaveStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    topicAiStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    selectedTopicId: 0,
    selectedIdeaId: 0,
    ideaSaveStatus: 'idle' as 'idle' | 'saving' | 'saved' | 'error',
    topicFilter: {
      status: 'all' as 'all' | TopicRecord['status'],
      priority: 'all' as 'all' | TopicRecord['priority'],
    },
    topicForm: {
      title: '',
      domain: '',
      keywords: '' as string,
      status: 'writable' as TopicRecord['status'],
      priority: 'medium' as TopicRecord['priority'],
      heat_score: 68,
      trend_summary: '',
      notes: '',
      ai_outline: '',
      linked_note_slug: '',
      linked_password_entry_ids: [] as number[],
      completed_note_slug: '',
      assignee_user_id: null as number | null,
      review_status: 'none' as TopicRecord['review_status'],
      due_date: '',
      source_type: 'manual' as TopicRecord['source_type'],
    },
    topicAiForm: {
      domain: '',
      keywords: '' as string,
      count: 5,
      save_to_pool: true,
    },
    topicDrafts: [] as TopicRecord[],
    ideaForm: {
      title: '',
      summary: '',
      details: '',
      idea_type: 'creative_idea' as IdeaRecord['idea_type'],
      tags: '' as string,
      status: 'pending_review' as IdeaRecord['status'],
      priority: 'medium' as IdeaRecord['priority'],
      value_score: 75,
      effort_score: 35,
      business_score: 60,
      linked_note_slug: '',
      linked_goal_id: null as number | null,
      linked_topic_id: null as number | null,
      source_context: '',
      visibility_scope: 'private' as IdeaRecord['visibility_scope'],
      assignee_user_id: null as number | null,
      next_step: '',
    },
    quickIdeaForm: {
      title: '',
      summary: '',
      idea_type: 'creative_idea' as IdeaRecord['idea_type'],
      priority: 'medium' as IdeaRecord['priority'],
    },
    clipUrl: '',
    clipSummarizeWithAi: true,
    clipUseAi: true,
    clipUseRag: false,
    clipSummaryPrompt: '请提炼网页的关键信息、结构脉络和可执行建议。',
    clipTargetFolder: '00_Inbox',
    clipPreview: null as ClipPreviewResult | null,
    showClipPreview: false,
    aiPrompt: '',
    aiResponse: '',
    aiReferences: [] as Array<{ title: string; note_slug?: string; snippet: string; score: string }>,
    aiUseRemote: true,
    aiUseRag: true,
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
    settingsModalOpen: false,
    settingsSection: 'instance' as 'instance' | 'spaces' | 'team' | 'models',
    uploadedDocument: null as DocumentUploadResult | null,
    showAiPreview: false,
    saveState: 'idle' as SaveState,
    activeTagFilter: '',
    recentVisited: JSON.parse(localStorage.getItem(recentVisitedStorageKey) ?? '[]') as string[],
    syncStatus: {
      pending_events: 0,
      connected_desktop_devices: 0,
      last_event_id: null,
    } as SyncStatus,
  }),
  getters: {
    sortedNotes(state) {
      const filtered = state.activeTagFilter
        ? state.notes.filter((note) => note.tags.includes(state.activeTagFilter))
        : state.notes
      return [...filtered].sort((left, right) => right.updated_at.localeCompare(left.updated_at))
    },
    recentVisitedNotes(state) {
      const noteMap = new Map(state.notes.map((note) => [note.slug, note]))
      return state.recentVisited.map((slug) => noteMap.get(slug)).filter(Boolean) as NoteRecord[]
    },
    isTeamEdition(state) {
      return state.instanceConfig?.edition === 'team'
    },
    isInstanceAdmin(state) {
      return state.currentUser?.role === 'admin'
    },
    selectableMembers(state) {
      return state.teamMembers.filter((member) => member.user_id !== state.currentUser?.id)
    },
      currentSpace(state) {
        return state.spaces.find((space) => space.slug === state.currentSpaceSlug) ?? null
      },
    selectedPasswordEntry(state) {
      return state.passwordEntries.find((entry) => entry.id === state.selectedPasswordEntryId) ?? null
    },
    topicPluginEnabled(state) {
      return state.topicOverview.plugin?.is_enabled ?? false
    },
    ideaPluginEnabled(state) {
      return state.ideaOverview.plugin?.is_enabled ?? false
    },
    selectedTopic(state) {
      return state.topicOverview.topics.find((topic) => topic.id === state.selectedTopicId) ?? null
    },
    selectedIdea(state) {
      return state.ideaOverview.ideas.find((idea) => idea.id === state.selectedIdeaId) ?? null
    },
  },
  actions: {
    persistRecentVisited() {
      localStorage.setItem(recentVisitedStorageKey, JSON.stringify(this.recentVisited.slice(0, 8)))
    },
    applyToken(token: string) {
      this.token = token
      this.isAuthenticated = true
      localStorage.setItem(tokenStorageKey, token)
      setAccessToken(token)
    },
    clearToken() {
      this.token = ''
      this.isAuthenticated = false
      this.currentUser = null
        this.currentSpaceSlug = ''
      localStorage.removeItem(tokenStorageKey)
      setAccessToken(null)
    },
    async bootstrap() {
      await this.loadInstanceConfig()
      if (this.token) {
        setAccessToken(this.token)
        try {
          const { data } = await apiClient.get('/auth/me')
          this.currentUser = data
          this.isAuthenticated = true
          await this.loadWorkspace()
          return
        } catch {
          this.clearToken()
        }
      }

      if (import.meta.env.DEV && !this.isAuthenticated) {
        try {
          await this.login()
        } catch {
          this.errorMessage = '本地自动登录失败，请确认后端已启动。'
        }
      }
    },
    async login() {
      this.isBusy = true
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/auth/login', {
          username: this.username,
          password: this.password,
        })
        this.applyToken(data.access_token)
        const currentUser = await apiClient.get('/auth/me')
        this.currentUser = currentUser.data
        await this.loadWorkspace()
      } catch (error) {
        this.errorMessage = '登录失败，请检查账号与服务端状态。'
        throw error
      } finally {
        this.isBusy = false
      }
    },
    async loadInstanceConfig() {
      const { data } = await apiClient.get('/instance/config')
      this.instanceConfig = data
      this.instanceBootstrapForm.instance_name = data.instance_name
      this.instanceBootstrapForm.deployment_mode = data.deployment_mode
      this.instanceBootstrapForm.edition = data.edition
    },
    async loadWorkspace() {
      const [tree, notes, tags, syncStatus, instanceConfig, spaces] = await Promise.all([
        apiClient.get('/tree'),
        apiClient.get('/notes'),
        apiClient.get('/tags'),
        apiClient.get('/sync/status'),
        apiClient.get('/instance/config'),
        apiClient.get('/instance/spaces'),
      ])
      this.fileTree = tree.data
      this.notes = notes.data
      this.tags = tags.data
      this.syncStatus = syncStatus.data
      this.instanceConfig = instanceConfig.data
      this.instanceBootstrapForm.instance_name = instanceConfig.data.instance_name
      this.instanceBootstrapForm.deployment_mode = instanceConfig.data.deployment_mode
      this.instanceBootstrapForm.edition = instanceConfig.data.edition
      this.spaces = spaces.data
        const preferredSpaceSlug = this.currentSpaceSlug || this.currentUser?.default_space_slug || spaces.data[0]?.slug || ''
        this.currentSpaceSlug = spaces.data.some((space: KnowledgeSpace) => space.slug === preferredSpaceSlug)
          ? preferredSpaceSlug
          : (spaces.data[0]?.slug ?? '')
      if (this.isTeamEdition && this.isInstanceAdmin) {
        await this.loadTeamMembers()
      }
      if (this.notes.length > 0) {
        await this.selectNote(this.notes[0].slug)
      }
      await this.loadAiProviderCatalog()
      await this.loadPluginData()
    },
    async selectNote(slug: string) {
      const [noteResponse, backlinksResponse] = await Promise.all([
        apiClient.get(`/notes/${slug}`),
        apiClient.get(`/links/${slug}`),
      ])
      this.currentNote = noteResponse.data
      this.backlinks = backlinksResponse.data
      await this.loadNoteSharing(slug)
      this.saveState = 'idle'
      this.recentVisited = [slug, ...this.recentVisited.filter((item) => item !== slug)].slice(0, 8)
      this.persistRecentVisited()
    },
    updateCurrentContent(content: string) {
      if (!this.currentNote) {
        return
      }
      this.currentNote = {
        ...this.currentNote,
        content,
      }
    },
    updateCurrentTitle(title: string) {
      if (!this.currentNote) {
        return
      }
      this.currentNote = {
        ...this.currentNote,
        title,
      }
    },
    markDirty() {
      this.saveState = 'dirty'
    },
    async saveCurrentNote() {
      if (!this.currentNote) {
        return
      }
      this.isBusy = true
      this.saveState = 'saving'
      try {
        const payload = {
          title: this.currentNote.title,
          content: this.currentNote.content,
          path: this.currentNote.path,
          tags: this.currentNote.tags,
          links: this.currentNote.links,
          previous_version: this.currentNote.version,
        }
        const { data } = await apiClient.put(`/notes/${this.currentNote.slug}`, payload)
        this.currentNote = data
        await this.refreshCollections(data.slug)
        this.saveState = 'saved'
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail?.message ?? '保存失败'
        this.saveState = 'error'
      } finally {
        this.isBusy = false
      }
    },
    async createQuickNote() {
      const now = new Date().toISOString().slice(0, 16).replace('T', ' ')
      const title = `新笔记 ${now}`
      const path = `01_Notes/${title}.md`
      const payload = {
        title,
        slug: title.toLowerCase().replace(/[^\w\u4e00-\u9fff-]+/g, '-'),
        content: `# ${title}\n\n在这里输入内容。`,
        path,
        space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
        tags: [],
        links: [],
        source_url: '',
      }
      const { data } = await apiClient.post('/notes', payload)
      await this.refreshCollections(data.slug)
      return data.slug as string
    },
    async createFolder(path: string) {
      await apiClient.post('/folders', { path })
      await this.refreshCollections(this.currentNote?.slug)
    },
    async renameFolder(sourcePath: string, targetPath: string) {
      await apiClient.put('/folders/move', { source_path: sourcePath, target_path: targetPath })
      await this.refreshCollections(this.currentNote?.slug)
    },
    async moveCurrentNote(targetPath: string) {
      if (!this.currentNote) {
        return
      }
      const { data } = await apiClient.put(`/notes/${this.currentNote.slug}/move`, {
        target_path: targetPath,
        previous_version: this.currentNote.version,
      })
      this.currentNote = data
      await this.refreshCollections(data.slug)
    },
    async refreshCollections(slug?: string) {
      const [tree, notes, tags, syncStatus, spaces] = await Promise.all([
        apiClient.get('/tree'),
        apiClient.get('/notes'),
        apiClient.get('/tags'),
        apiClient.get('/sync/status'),
        apiClient.get('/instance/spaces'),
      ])
      this.fileTree = tree.data
      this.notes = notes.data
      this.tags = tags.data
      this.syncStatus = syncStatus.data
      this.spaces = spaces.data
      await this.loadPluginData()
      if (slug) {
        await this.selectNote(slug)
      }
    },
    async loadPluginData() {
      const [pluginConfigs, passwordConfig, passwordEntries, goalOverview, topicOverview, ideaOverview] = await Promise.all([
        apiClient.get('/plugins/configs'),
        apiClient.get('/passwords/config'),
        apiClient.get('/passwords'),
        apiClient.get('/goals/overview'),
        apiClient.get('/topics/overview'),
        apiClient.get('/ideas/overview'),
      ])
      this.pluginConfigs = pluginConfigs.data
      this.passwordVaultConfig = passwordConfig.data
      this.passwordEntries = passwordEntries.data
      this.goalOverview = goalOverview.data
      this.topicOverview = topicOverview.data
      this.ideaOverview = ideaOverview.data
      if (!this.selectedPasswordEntryId && this.passwordEntries.length > 0) {
        this.selectedPasswordEntryId = this.passwordEntries[0].id
      }
      if (!this.selectedTopicId && this.topicOverview.topics.length > 0) {
        this.selectedTopicId = this.topicOverview.topics[0].id
      }
      if (!this.selectedIdeaId && this.ideaOverview.ideas.length > 0) {
        this.selectedIdeaId = this.ideaOverview.ideas[0].id
      }
      if (!this.goalPlanForm.goal_id && this.goalOverview.goals[0]) {
        this.goalPlanForm.goal_id = this.goalOverview.goals[0].id
      }
      if (!this.goalTaskForm.goal_id && this.goalOverview.goals[0]) {
        this.goalTaskForm.goal_id = this.goalOverview.goals[0].id
      }
      if (!this.goalTaskForm.plan_id && this.goalOverview.plans[0]) {
        this.goalTaskForm.plan_id = this.goalOverview.plans[0].id
      }
      if (!this.goalJournalForm.goal_id && this.goalOverview.goals[0]) {
        this.goalJournalForm.goal_id = this.goalOverview.goals[0].id
      }
      if (!this.goalJournalForm.plan_id && this.goalOverview.plans[0]) {
        this.goalJournalForm.plan_id = this.goalOverview.plans[0].id
      }
      if (this.goalOverview.today_tasks.length > 0) {
        this.goalJournalForm.task_ids = this.goalOverview.today_tasks.map((task) => task.id)
      }
    },
    populateTopicForm(topicId?: number) {
      const topic = this.topicOverview.topics.find((item) => item.id === topicId)
      if (!topic) {
        this.topicForm = {
          title: '',
          domain: this.currentSpace?.name || '',
          keywords: '',
          status: 'writable',
          priority: 'medium',
          heat_score: 68,
          trend_summary: '',
          notes: '',
          ai_outline: '',
          linked_note_slug: '',
          linked_password_entry_ids: [],
          completed_note_slug: '',
          assignee_user_id: null,
          review_status: 'none',
          due_date: '',
          source_type: 'manual',
        }
        return
      }
      this.selectedTopicId = topic.id
      this.topicForm = {
        title: topic.title,
        domain: topic.domain,
        keywords: topic.keywords.join('\n'),
        status: topic.status,
        priority: topic.priority,
        heat_score: topic.heat_score,
        trend_summary: topic.trend_summary,
        notes: topic.notes,
        ai_outline: topic.ai_outline,
        linked_note_slug: topic.linked_note_slug,
        linked_password_entry_ids: [...topic.linked_password_entry_ids],
        completed_note_slug: topic.completed_note_slug,
        assignee_user_id: topic.assignee_user_id ?? null,
        review_status: topic.review_status,
        due_date: topic.due_date ? topic.due_date.slice(0, 10) : '',
        source_type: topic.source_type,
      }
    },
    async togglePlugin(pluginId: 'topics' | 'ideas', isEnabled: boolean) {
      await apiClient.put(`/plugins/configs/${pluginId}`, { is_enabled: isEnabled })
      await this.loadPluginData()
    },
    populateIdeaForm(ideaId?: number) {
      const idea = this.ideaOverview.ideas.find((item) => item.id === ideaId)
      if (!idea) {
        this.ideaForm = {
          title: '',
          summary: '',
          details: '',
          idea_type: 'creative_idea',
          tags: '',
          status: 'pending_review',
          priority: 'medium',
          value_score: 75,
          effort_score: 35,
          business_score: 60,
          linked_note_slug: '',
          linked_goal_id: null,
          linked_topic_id: null,
          source_context: '',
          visibility_scope: this.isTeamEdition ? 'team' : 'private',
          assignee_user_id: null,
          next_step: '',
        }
        return
      }
      this.selectedIdeaId = idea.id
      this.ideaForm = {
        title: idea.title,
        summary: idea.summary,
        details: idea.details,
        idea_type: idea.idea_type,
        tags: idea.tags.join('\n'),
        status: idea.status,
        priority: idea.priority,
        value_score: idea.value_score,
        effort_score: idea.effort_score,
        business_score: idea.business_score,
        linked_note_slug: idea.linked_note_slug,
        linked_goal_id: idea.linked_goal_id ?? null,
        linked_topic_id: idea.linked_topic_id ?? null,
        source_context: idea.source_context,
        visibility_scope: idea.visibility_scope,
        assignee_user_id: idea.assignee_user_id ?? null,
        next_step: idea.next_step,
      }
    },
    async saveIdea(ideaId?: number) {
      this.ideaSaveStatus = 'saving'
      this.errorMessage = ''
      const payload = {
        ...this.ideaForm,
        tags: this.ideaForm.tags.split('\n').map((item) => item.trim()).filter(Boolean),
        space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
      }
      try {
        if (ideaId) {
          await apiClient.put(`/ideas/${ideaId}`, payload)
        } else {
          await apiClient.post('/ideas', payload)
        }
        this.ideaSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '创意保存失败'
        this.ideaSaveStatus = 'error'
      }
    },
    async captureQuickIdea() {
      this.ideaSaveStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post('/ideas', {
          title: this.quickIdeaForm.title,
          summary: this.quickIdeaForm.summary,
          details: this.quickIdeaForm.summary,
          idea_type: this.quickIdeaForm.idea_type,
          tags: [],
          status: 'pending_review',
          priority: this.quickIdeaForm.priority,
          value_score: 72,
          effort_score: 35,
          business_score: 60,
          linked_note_slug: this.currentNote?.slug ?? '',
          linked_goal_id: null,
          linked_topic_id: null,
          source_context: this.currentNote?.title || this.currentSpace?.name || '工作台快速捕捉',
          visibility_scope: this.isTeamEdition ? 'team' : 'private',
          assignee_user_id: null,
          next_step: '补充评估信息',
          space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
        })
        this.quickIdeaForm = {
          title: '',
          summary: '',
          idea_type: 'creative_idea',
          priority: 'medium',
        }
        this.ideaSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '快速录入失败'
        this.ideaSaveStatus = 'error'
      }
    },
    async deleteIdea(ideaId: number) {
      await apiClient.delete(`/ideas/${ideaId}`)
      if (this.selectedIdeaId === ideaId) {
        this.selectedIdeaId = 0
        this.populateIdeaForm()
      }
      await this.loadPluginData()
    },
    async updateIdeaStatus(ideaId: number, status: IdeaRecord['status']) {
      this.ideaSaveStatus = 'saving'
      try {
        await apiClient.post(`/ideas/${ideaId}/status`, { status })
        this.ideaSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '创意状态更新失败'
        this.ideaSaveStatus = 'error'
      }
    },
    async saveTopic(topicId?: number) {
      this.topicSaveStatus = 'saving'
      this.errorMessage = ''
      const payload = {
        ...this.topicForm,
        keywords: this.topicForm.keywords.split('\n').map((item) => item.trim()).filter(Boolean),
        space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
      }
      try {
        if (topicId) {
          await apiClient.put(`/topics/${topicId}`, payload)
        } else {
          await apiClient.post('/topics', payload)
        }
        this.topicSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '选题保存失败'
        this.topicSaveStatus = 'error'
      }
    },
    async deleteTopic(topicId: number) {
      await apiClient.delete(`/topics/${topicId}`)
      if (this.selectedTopicId === topicId) {
        this.selectedTopicId = 0
        this.populateTopicForm()
      }
      await this.loadPluginData()
    },
    async updateTopicStatus(topicId: number, status: TopicRecord['status']) {
      this.topicSaveStatus = 'saving'
      try {
        await apiClient.post(`/topics/${topicId}/status`, { status })
        this.topicSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '选题状态更新失败'
        this.topicSaveStatus = 'error'
      }
    },
    async discoverTopics() {
      this.topicAiStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/topics/ai/discover', {
          domain: this.topicAiForm.domain,
          keywords: this.topicAiForm.keywords.split('\n').map((item) => item.trim()).filter(Boolean),
          count: this.topicAiForm.count,
          save_to_pool: this.topicAiForm.save_to_pool,
          use_ai: this.aiUseRemote,
          space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
        })
        this.topicDrafts = data.topics
        this.topicAiStatus = 'saved'
        if (this.topicAiForm.save_to_pool) {
          await this.loadPluginData()
        }
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? 'AI 选题生成失败'
        this.topicAiStatus = 'error'
      }
    },
    async generateTopicOutline() {
      this.topicAiStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/topics/ai/outline', {
          title: this.topicForm.title,
          domain: this.topicForm.domain,
          keywords: this.topicForm.keywords.split('\n').map((item) => item.trim()).filter(Boolean),
          trend_summary: this.topicForm.trend_summary,
          use_ai: this.aiUseRemote,
        })
        this.topicForm.ai_outline = data.outline
        this.topicAiStatus = 'saved'
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '选题大纲生成失败'
        this.topicAiStatus = 'error'
      }
    },
    async deleteCurrentNote() {
      if (!this.currentNote) {
        return
      }
      this.isBusy = true
      try {
        await apiClient.delete(`/notes/${this.currentNote.slug}`, {
          data: { previous_version: this.currentNote.version },
        })
        this.currentNote = null
        this.aiResponse = ''
        this.aiReferences = []
        this.saveState = 'idle'
        await this.refreshCollections(this.notes[0]?.slug)
      } catch {
        this.errorMessage = '删除失败'
        this.saveState = 'error'
      } finally {
        this.isBusy = false
      }
    },
    async performSearch() {
      if (this.searchQuery.trim().length < 2) {
        this.searchResults = []
        return
      }
      const { data } = await apiClient.get('/search/unified', { params: { q: this.searchQuery } })
      this.searchResults = data.results
    },
    selectPasswordEntry(entryId: number) {
      this.selectedPasswordEntryId = entryId
      this.revealedPassword = ''
      void this.loadPasswordAudits(entryId)
    },
    populatePasswordForm(entryId?: number) {
      const entry = this.passwordEntries.find((item) => item.id === entryId)
      if (!entry) {
        this.passwordForm = {
          title: '',
          username: '',
          password: '',
          master_password: this.passwordForm.master_password,
          category: 'general',
          url: '',
          website: '',
          notes: '',
          linked_note_slug: '',
          vault_scope: 'private',
          shared_member_ids: [],
          editor_member_ids: [],
        }
        return
      }
      this.passwordForm = {
        title: entry.title,
        username: entry.username,
        password: '',
        master_password: this.passwordForm.master_password,
        category: entry.category,
        url: entry.url,
        website: entry.website,
        notes: entry.notes,
        linked_note_slug: entry.linked_note_slug,
        vault_scope: entry.vault_scope,
        shared_member_ids: [...entry.shared_member_ids],
        editor_member_ids: [...entry.editor_member_ids],
      }
    },
    async setupPasswordVault() {
      this.passwordSetupStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post('/passwords/config/setup', { master_password: this.passwordForm.master_password })
        this.passwordVaultConfig = { is_initialized: true }
        this.passwordSetupStatus = 'saved'
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '密码库初始化失败'
        this.passwordSetupStatus = 'error'
      }
    },
    async verifyPasswordVault() {
      this.passwordVerifyStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/passwords/config/verify', { master_password: this.passwordForm.master_password })
        this.passwordVerifyStatus = data.verified ? 'saved' : 'error'
        if (!data.verified) {
          this.errorMessage = '主密码验证失败'
        }
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '主密码验证失败'
        this.passwordVerifyStatus = 'error'
      }
    },
    async generatePassword() {
      const { data } = await apiClient.post('/passwords/generate', this.passwordGeneratorForm)
      this.passwordForm.password = data.password
    },
    async savePasswordEntry(entryId?: number) {
      this.passwordSaveStatus = 'saving'
      this.errorMessage = ''
      try {
        if (entryId) {
          await apiClient.put(`/passwords/${entryId}`, this.passwordForm)
        } else {
          await apiClient.post('/passwords', {
            ...this.passwordForm,
            space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
          })
        }
        this.passwordSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '密码条目保存失败'
        this.passwordSaveStatus = 'error'
      }
    },
    async revealPassword(entryId: number) {
      this.passwordVerifyStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post(`/passwords/${entryId}/reveal`, {
          master_password: this.passwordForm.master_password,
        })
        this.revealedPassword = data.password
        this.passwordVerifyStatus = 'saved'
        await this.loadPasswordAudits(entryId)
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '读取密码失败'
        this.passwordVerifyStatus = 'error'
      }
    },
    async deletePasswordEntry(entryId: number) {
      await apiClient.delete(`/passwords/${entryId}`)
      if (this.selectedPasswordEntryId === entryId) {
        this.selectedPasswordEntryId = 0
        this.revealedPassword = ''
      }
      await this.loadPluginData()
    },
    async recordPasswordCopy(entryId: number) {
      await apiClient.post(`/passwords/${entryId}/copy`)
      await this.loadPasswordAudits(entryId)
    },
    async loadPasswordAudits(entryId: number) {
      const { data } = await apiClient.get(`/passwords/${entryId}/audits`)
      this.passwordAudits = data
    },
    async submitClip() {
      if (!this.clipUrl.trim()) {
        return
      }
      this.isBusy = true
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/clip', {
          url: this.clipUrl,
          device_id: 'web-clipper',
          summarize_with_ai: this.clipSummarizeWithAi,
          use_ai: this.clipUseAi,
          use_rag: this.clipUseRag,
          summary_prompt: this.clipSummaryPrompt,
          target_folder: this.clipTargetFolder,
          space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
          save_to_note: false,
        }, {
          timeout: 60000,
        })
        this.clipPreview = data
        this.showClipPreview = true
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '解析失败，请确认目标网址可访问。'
      } finally {
        this.isBusy = false
      }
    },
    closeClipPreview() {
      this.showClipPreview = false
      this.clipPreview = null
    },
    async saveClipPreview() {
      if (!this.clipPreview) {
        return
      }
      this.isBusy = true
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/clip/save-preview', {
          title: this.clipPreview.extracted_title,
          source_url: this.clipPreview.source_url,
          content: this.clipPreview.preview_content,
          summary: this.clipPreview.summary,
          target_folder: this.clipTargetFolder,
          space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
          device_id: 'web-clipper',
        })
        this.clipUrl = ''
        this.closeClipPreview()
        if (data.note?.slug) {
          await this.refreshCollections(data.note.slug)
        }
      } catch {
        this.errorMessage = '保存笔记失败，请稍后重试。'
      } finally {
        this.isBusy = false
      }
    },
    async loadAiProviderCatalog() {
      const { data } = await apiClient.get('/ai/providers/catalog')
      this.aiProviderPresets = data.presets
      this.aiProviderCurrent = data.current
      if (data.current) {
        this.aiProviderForm.provider_id = data.current.provider_id
        this.aiProviderForm.provider_label = data.current.provider_label
        this.aiProviderForm.base_url = data.current.base_url
        this.aiProviderForm.model_name = data.current.model_name
        this.aiProviderForm.is_enabled = data.current.is_enabled
      } else if (data.presets[0]) {
        const preset = data.presets[0]
        this.applyAiProviderPreset(preset.provider_id)
      }
    },
    applyAiProviderPreset(providerId: string) {
      const preset = this.aiProviderPresets.find((item) => item.provider_id === providerId)
      if (!preset) {
        return
      }
      this.aiProviderForm.provider_id = preset.provider_id
      this.aiProviderForm.provider_label = preset.label
      this.aiProviderForm.base_url = preset.base_url
      this.aiProviderForm.model_name = preset.models[0] ?? ''
    },
    async saveAiProviderConfig() {
      this.aiProviderSaveStatus = 'saving'
      this.errorMessage = ''
      try {
        const providerId = this.aiProviderForm.provider_id || 'custom'
        const providerLabel = this.aiProviderForm.provider_label || '自定义'
        const { data } = await apiClient.put('/ai/providers/current', {
          provider_id: providerId,
          provider_label: providerLabel,
          base_url: this.aiProviderForm.base_url,
          api_key: this.aiProviderForm.api_key,
          model_name: this.aiProviderForm.model_name,
          is_enabled: true,
        })
        this.aiProviderCurrent = data
        this.aiProviderForm.provider_id = data.provider_id
        this.aiProviderForm.provider_label = data.provider_label
        this.aiProviderForm.base_url = data.base_url
        this.aiProviderForm.model_name = data.model_name
        this.aiProviderForm.is_enabled = true
        this.aiProviderForm.api_key = ''
        this.aiProviderSaveStatus = 'saved'
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '模型配置保存失败'
        this.aiProviderSaveStatus = 'error'
      }
    },
    async bootstrapInstance() {
      this.instanceBootstrapStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.put('/instance/bootstrap', this.instanceBootstrapForm)
        this.instanceConfig = data
        this.instanceBootstrapStatus = 'saved'
        await this.loadWorkspace()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '实例配置保存失败'
        this.instanceBootstrapStatus = 'error'
      }
    },
    async loadSpaces() {
      const { data } = await apiClient.get('/instance/spaces')
      this.spaces = data
        if (!this.spaces.some((space: KnowledgeSpace) => space.slug === this.currentSpaceSlug)) {
          this.currentSpaceSlug = this.currentUser?.default_space_slug || this.spaces[0]?.slug || ''
        }
    },
      setCurrentSpace(spaceSlug: string) {
        if (this.spaces.some((space) => space.slug === spaceSlug)) {
          this.currentSpaceSlug = spaceSlug
        }
      },
    async createSpace() {
      this.createSpaceStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post('/instance/spaces', this.createSpaceForm)
        this.createSpaceStatus = 'saved'
        this.createSpaceForm = {
          name: '',
          slug: '',
          visibility: 'private',
        }
        await this.loadSpaces()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '创建空间失败'
        this.createSpaceStatus = 'error'
      }
    },
    async loadTeamMembers() {
      if (!this.isTeamEdition || !this.isInstanceAdmin) {
        this.teamMembers = []
        return
      }
      const { data } = await apiClient.get('/instance/team/members')
      this.teamMembers = data
    },
    async createTeamInvite() {
      this.teamInviteStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/instance/team/invites', this.teamInviteForm)
        this.latestInvite = data
        this.teamInviteStatus = 'saved'
        await this.loadTeamMembers()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '创建邀请失败'
        this.teamInviteStatus = 'error'
      }
    },
      async acceptTeamInvite() {
        this.teamInviteAcceptStatus = 'saving'
        this.errorMessage = ''
        try {
          await apiClient.post('/instance/team/invites/accept', this.teamInviteAcceptForm)
          this.teamInviteAcceptStatus = 'saved'
          this.username = this.teamInviteAcceptForm.username
          this.password = this.teamInviteAcceptForm.password
          await this.login()
          this.teamInviteAcceptForm = {
            invite_code: '',
            username: '',
            password: '',
            display_name: '',
          }
        } catch (error: any) {
          this.errorMessage = error?.response?.data?.detail ?? '接受邀请失败'
          this.teamInviteAcceptStatus = 'error'
        }
      },
    async loadNoteSharing(slug: string) {
      this.noteSharing = null
      if (!this.isTeamEdition) {
        return
      }
      try {
        const { data } = await apiClient.get(`/notes/${slug}/sharing`)
        this.noteSharing = data
      } catch {
        this.noteSharing = null
      }
    },
    async saveNoteSharing() {
      if (!this.currentNote || !this.noteSharing) {
        return
      }
      this.noteSharingStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.put(`/notes/${this.currentNote.slug}/sharing`, {
          visibility_scope: this.noteSharing.visibility_scope,
          shared_member_ids: this.noteSharing.shared_member_ids,
        })
        this.noteSharing = data
        this.noteSharingStatus = 'saved'
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '笔记公开范围保存失败'
        this.noteSharingStatus = 'error'
      }
    },
    openSettings(section: 'instance' | 'spaces' | 'team' | 'models' = 'instance') {
      this.settingsSection = section
      this.settingsModalOpen = true
    },
    closeSettings() {
      this.settingsModalOpen = false
    },
    async uploadDocument(file: File, options?: { summarizeWithAi?: boolean; saveToNote?: boolean; targetFolder?: string }) {
      const form = new FormData()
      form.append('file', file)
      form.append('summarize_with_ai', String(options?.summarizeWithAi ?? true))
      form.append('use_ai', String(this.aiUseRemote))
      form.append('use_rag', String(this.aiUseRag))
      form.append('save_to_note', String(options?.saveToNote ?? true))
      form.append('target_folder', options?.targetFolder ?? '03_Resources')
        form.append('space_slug', this.currentSpaceSlug || this.currentUser?.default_space_slug || '')
      form.append('summary_prompt', '请提炼这份资料的核心观点、结构脉络和可执行建议。')
      const { data } = await apiClient.post('/documents/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      this.uploadedDocument = data
      if (data.saved_note?.slug) {
        await this.refreshCollections(data.saved_note.slug)
      }
    },
    async runAi(action: 'summary' | 'expand' | 'polish' | 'qa' | 'create', saveAsNote = false) {
      this.isBusy = true
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/ai/generate', {
          action,
          note_slug: this.currentNote?.slug,
          prompt: this.aiPrompt || '请基于当前笔记和知识库给出结果',
          save_as_note: saveAsNote,
          target_folder: '00_Inbox',
            space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
          use_ai: this.aiUseRemote,
          use_rag: this.aiUseRag,
        })
        this.aiResponse = data.content
        this.aiReferences = data.references
        this.showAiPreview = true
        if (data.saved_note) {
          await this.refreshCollections(data.saved_note.slug)
        }
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? 'AI 调用失败，请检查服务端配置。'
      } finally {
        this.isBusy = false
      }
    },
    async pollSyncStatus() {
      if (!this.isAuthenticated) {
        return
      }
      const { data } = await apiClient.get('/sync/status')
      this.syncStatus = data
    },
    applyAiResultToCurrentNote() {
      if (!this.currentNote || !this.aiResponse) {
        return
      }
      this.currentNote = {
        ...this.currentNote,
        content: `${this.currentNote.content}\n\n---\n\n${this.aiResponse}`,
      }
      this.showAiPreview = false
      this.saveState = 'dirty'
    },
    setTagFilter(tag: string) {
      this.activeTagFilter = this.activeTagFilter === tag ? '' : tag
    },
    clearTagFilter() {
      this.activeTagFilter = ''
    },
    async generateGoalPlanDraft() {
      this.goalAiPlanStatus = 'saving'
      this.errorMessage = ''
      try {
        const { data } = await apiClient.post('/goals/ai/plan', {
          title: this.goalForm.title,
          vision: this.goalForm.vision,
          key_results: this.goalForm.key_results.split('\n').map((item) => item.trim()).filter(Boolean),
          use_ai: this.aiUseRemote,
        })
        this.goalAiPlanDraft = data.plans
        this.goalAiPlanStatus = 'saved'
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? 'AI 拆解失败'
        this.goalAiPlanStatus = 'error'
      }
    },
    async createGoal() {
      this.goalSaveStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post('/goals', {
          ...this.goalForm,
          key_results: this.goalForm.key_results.split('\n').map((item) => item.trim()).filter(Boolean),
          space_slug: this.currentSpaceSlug || this.currentUser?.default_space_slug || undefined,
        })
        this.goalSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '目标创建失败'
        this.goalSaveStatus = 'error'
      }
    },
    async createGoalPlan() {
      if (!this.goalPlanForm.goal_id) {
        return
      }
      this.goalSaveStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post(`/goals/${this.goalPlanForm.goal_id}/plans`, this.goalPlanForm)
        this.goalSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '阶段计划创建失败'
        this.goalSaveStatus = 'error'
      }
    },
    async createGoalTask() {
      this.goalTaskStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post('/goal-tasks', this.goalTaskForm)
        this.goalTaskStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '任务创建失败'
        this.goalTaskStatus = 'error'
      }
    },
    async toggleGoalTask(taskId: number, done: boolean) {
      this.goalTaskStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.post(`/goal-tasks/${taskId}/toggle`, { done })
        this.goalTaskStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '任务状态更新失败'
        this.goalTaskStatus = 'error'
      }
    },
    async saveGoalJournal() {
      this.goalSaveStatus = 'saving'
      this.errorMessage = ''
      try {
        await apiClient.put('/goal-journals', this.goalJournalForm)
        this.goalSaveStatus = 'saved'
        await this.loadPluginData()
      } catch (error: any) {
        this.errorMessage = error?.response?.data?.detail ?? '复盘保存失败'
        this.goalSaveStatus = 'error'
      }
    },
  },
})
