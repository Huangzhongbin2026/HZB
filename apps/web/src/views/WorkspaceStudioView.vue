<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

import { apiClient } from '../api/client'
import GraphMapPanel from '../components/GraphMapPanel.vue'
import TreeNode from '../components/TreeNode.vue'
import { useWorkspaceStore } from '../stores/workspace'

type WorkspaceMode = 'home' | 'notes' | 'graph' | 'ai' | 'whiteboard' | 'daily' | 'templates' | 'settings' | 'passwords' | 'topics' | 'ideas'
type RightTab = 'outline' | 'links' | 'tags' | 'properties'
type ShortcutKey = 'commandPalette' | 'toggleLeft' | 'toggleRight' | 'newNote' | 'dailyNote' | 'quickIdea'

type BoardCard = {
  id: string
  title: string
  body: string
  x: number
  y: number
  color: string
}

const route = useRoute()
const router = useRouter()
const store = useWorkspaceStore()
const {
  aiPrompt,
  aiProviderCurrent,
  aiProviderForm,
  aiProviderPresets,
  aiReferences,
  aiResponse,
  aiUseRag,
  aiUseRemote,
  backlinks,
  clipPreview,
  clipSummarizeWithAi,
  clipSummaryPrompt,
  clipUrl,
  currentSpace,
  currentNote,
  errorMessage,
  fileTree,
  isAuthenticated,
  isBusy,
  recentVisitedNotes,
  saveState,
  searchQuery,
  searchResults,
  selectedPasswordEntry,
  selectedTopic,
  settingsModalOpen,
  showAiPreview,
  showClipPreview,
  sortedNotes,
  syncStatus,
  tags,
  goalAiPlanDraft,
  goalForm,
  goalJournalForm,
  goalOverview,
  goalPlanForm,
  goalTaskForm,
  pluginConfigs,
  passwordAudits,
  passwordEntries,
  passwordForm,
  passwordGeneratorForm,
  passwordSetupStatus,
  passwordVaultConfig,
  passwordVerifyStatus,
  revealedPassword,
  topicAiForm,
  topicAiStatus,
  topicDrafts,
  topicForm,
  topicOverview,
  topicSaveStatus,
  ideaForm,
  ideaOverview,
  ideaSaveStatus,
  quickIdeaForm,
  selectedIdea,
  uploadedDocument,
  username,
  password,
} = storeToRefs(store)

const explorerStorageKey = 'knowledge-cloud-web-explorer'
const inspectorStorageKey = 'knowledge-cloud-web-inspector'
const shortcutStorageKey = 'knowledge-cloud-web-shortcuts'
const boardStorageKey = 'knowledge-cloud-web-board-cards'
const commandOpen = ref(false)
const commandQuery = ref('')
const quickIdeaOpen = ref(false)
const explorerCollapsed = ref(localStorage.getItem(explorerStorageKey) === 'collapsed')
const inspectorCollapsed = ref(localStorage.getItem(inspectorStorageKey) === 'collapsed')
const rightTab = ref<RightTab>('outline')
const createFolderPath = ref('01_Notes/')
const boardCards = ref<BoardCard[]>(JSON.parse(localStorage.getItem(boardStorageKey) ?? '[]'))
const shortcutDraft = reactive<Record<ShortcutKey, string>>({
  commandPalette: 'Ctrl+K',
  toggleLeft: 'Ctrl+B',
  toggleRight: 'Ctrl+.',
  newNote: 'Ctrl+N',
  dailyNote: 'Ctrl+D',
  quickIdea: 'Ctrl+Shift+I',
  ...JSON.parse(localStorage.getItem(shortcutStorageKey) ?? '{}'),
})

const routeMode = computed<WorkspaceMode>(() => {
  const name = route.name
  if (name === 'graph') {
    return 'graph'
  }
  if (name === 'ai') {
    return 'ai'
  }
  if (name === 'whiteboard') {
    return 'whiteboard'
  }
  if (name === 'daily') {
    return 'daily'
  }
  if (name === 'templates') {
    return 'templates'
  }
  if (name === 'settings') {
    return 'settings'
  }
  if (name === 'passwords') {
    return 'passwords'
  }
  if (name === 'topics') {
    return 'topics'
  }
  if (name === 'ideas') {
    return 'ideas'
  }
  if (name === 'notes') {
    return 'notes'
  }
  return 'home'
})

const topicPluginEnabled = computed(() => pluginConfigs.value.some((item) => item.plugin_id === 'topics' && item.is_enabled))
const ideaPluginEnabled = computed(() => pluginConfigs.value.some((item) => item.plugin_id === 'ideas' && item.is_enabled))

const railItems = computed(() => {
  const items = [
    { id: 'home' as const, to: '/', label: '主页' },
    { id: 'passwords' as const, to: '/passwords', label: '密码' },
    { id: 'notes' as const, to: '/notes', label: '文件' },
    { id: 'daily' as const, to: '/daily', label: '日记' },
    { id: 'templates' as const, to: '/templates', label: '模板' },
    { id: 'whiteboard' as const, to: '/whiteboard', label: '白板' },
    { id: 'graph' as const, to: '/graph', label: '图谱' },
    { id: 'ai' as const, to: '/ai', label: '创作' },
  ]
  if (topicPluginEnabled.value) {
    items.splice(1, 0, { id: 'topics' as const, to: '/topics', label: '选题' })
  }
  if (ideaPluginEnabled.value) {
    items.splice(2, 0, { id: 'ideas' as const, to: '/ideas', label: '点子' })
  }
  items.push({ id: 'settings' as const, to: '/settings', label: '设置' })
  return items
})

const noteOutline = computed(() => {
  if (!currentNote.value) {
    return [] as Array<{ level: number; text: string }>
  }
  return currentNote.value.content
    .split('\n')
    .map((line) => /^(#{1,6})\s+(.+)$/.exec(line))
    .filter(Boolean)
    .map((match) => ({ level: match![1].length, text: match![2] }))
})

const recentNotes = computed(() => recentVisitedNotes.value.length > 0 ? recentVisitedNotes.value.slice(0, 5) : sortedNotes.value.slice(0, 5))
const dailyNotes = computed(() => sortedNotes.value.filter((note) => note.path.startsWith('05_Daily/') || note.path.startsWith('06_Diary/')))
const templateNotes = computed(() => sortedNotes.value.filter((note) => note.path.startsWith('04_Templates/')))

const commandItems = computed(() => [
  { id: 'new-note', title: '新建笔记', shortcut: shortcutDraft.newNote, run: () => store.createQuickNote().then((slug) => router.push(`/notes/${slug}`)) },
  { id: 'new-daily', title: '创建今日日记', shortcut: shortcutDraft.dailyNote, run: () => createTodayDailyNote() },
  { id: 'toggle-left', title: '折叠左侧目录', shortcut: shortcutDraft.toggleLeft, run: () => toggleExplorer() },
  { id: 'toggle-right', title: '折叠右侧扩展栏', shortcut: shortcutDraft.toggleRight, run: () => toggleInspector() },
  { id: 'open-whiteboard', title: '打开白板', shortcut: '', run: () => router.push('/whiteboard') },
  { id: 'open-templates', title: '打开模板', shortcut: '', run: () => router.push('/templates') },
  { id: 'open-settings', title: '打开设置', shortcut: '', run: () => router.push('/settings') },
  { id: 'open-passwords', title: '打开密码管家', shortcut: '', run: () => router.push('/passwords') },
  ...(topicPluginEnabled.value ? [{ id: 'open-topics', title: '打开选题工作台', shortcut: '', run: () => router.push('/topics') }] : []),
  ...(ideaPluginEnabled.value ? [{ id: 'open-ideas', title: '打开创意需求工作台', shortcut: '', run: () => router.push('/ideas') }] : []),
  ...(ideaPluginEnabled.value ? [{ id: 'quick-idea', title: '快速捕捉创意', shortcut: shortcutDraft.quickIdea, run: () => openQuickIdeaCapture() }] : []),
  { id: 'clip-link', title: '解析当前链接', shortcut: '', run: () => store.submitClip() },
  { id: 'run-ai-create', title: '创作并保存笔记', shortcut: '', run: () => store.runAi('create', true) },
])

const workspaceTitle = computed(() => {
  if (routeMode.value === 'graph') {
    return '关系图谱'
  }
  if (routeMode.value === 'passwords') {
    return selectedPasswordEntry.value?.title || '密码管家'
  }
  if (routeMode.value === 'topics') {
    return selectedTopic.value?.title || '内容选题工作台'
  }
  if (routeMode.value === 'ideas') {
    return selectedIdea.value?.title || '创意点子与需求工作台'
  }
  if (routeMode.value === 'home') {
    return '个人知识库工作台'
  }
  return currentNote.value?.title || '个人知识库工作台'
})

const workspaceSubtitle = computed(() => {
  if (routeMode.value === 'graph') {
    return '按双向链接和出链关系显示知识网络，支持节点聚焦、缩放和关系热点查看。'
  }
  if (routeMode.value === 'passwords') {
    return selectedPasswordEntry.value?.website || '主密码用于派生加密密钥，密码条目可参与全局搜索与团队共享。'
  }
  if (routeMode.value === 'topics') {
    return topicPluginEnabled.value
      ? `完成率 ${topicOverview.value.stats.completion_rate}% · 待写 ${topicOverview.value.stats.writable + topicOverview.value.stats.pending} 个 · 选题与笔记、密码条目双向联动。`
      : '在设置里开启选题插件后，这里会成为独立的创作选题工作台。'
  }
  if (routeMode.value === 'ideas') {
    return ideaPluginEnabled.value
      ? `待评估 ${ideaOverview.value.stats.pending_review} 个 · 规划中 ${ideaOverview.value.stats.planning} 个 · 机会均分 ${ideaOverview.value.stats.average_opportunity_score}。`
      : '在设置里开启创意点子与需求管理插件后，这里会提供快速捕捉、状态流转和知识库联动能力。'
  }
  if (routeMode.value === 'home') {
    return `当前空间：${currentSpace.value?.name || '未选择'} · 首页优先呈现目标计划与今日执行。`
  }
  return currentNote.value?.path || `当前空间：${currentSpace.value?.name || '未选择'} · 参考 Obsidian 的信息密度、侧栏结构和命令面板模式重构。`
})

const activeGoal = computed(() => goalOverview.value.goals.find((goal) => goal.status === 'active') || goalOverview.value.goals[0] || null)
const activeGoalPlans = computed(() => goalOverview.value.plans.filter((plan) => plan.goal_id === activeGoal.value?.id))
const filteredTopics = computed(() => topicOverview.value.topics)
const filteredIdeas = computed(() =>
  [...ideaOverview.value.ideas].sort((left, right) => right.opportunity_score - left.opportunity_score || right.updated_at.localeCompare(left.updated_at)),
)

function openSearchItem(item: { kind: 'note' | 'password'; note_slug?: string; password_id?: number }) {
  if (item.kind === 'password' && item.password_id) {
    store.selectPasswordEntry(item.password_id)
    store.populatePasswordForm(item.password_id)
    void router.push('/passwords')
    return
  }
  if (item.note_slug) {
    void router.push(`/notes/${item.note_slug}`)
  }
}

async function copyRevealedPassword(entryId: number) {
  if (!revealedPassword.value) {
    return
  }
  await navigator.clipboard.writeText(revealedPassword.value)
  await store.recordPasswordCopy(entryId)
}

function applyGoalDraft(index: number) {
  const draft = goalAiPlanDraft.value[index]
  if (!draft || !activeGoal.value) {
    return
  }
  goalPlanForm.value.goal_id = activeGoal.value.id
  goalPlanForm.value.title = draft.title
  goalPlanForm.value.summary = draft.summary
  if (draft.tasks[0]) {
    goalTaskForm.value.goal_id = activeGoal.value.id
    goalTaskForm.value.title = draft.tasks[0]
  }
}

const visibleCommands = computed(() => {
  const keyword = commandQuery.value.trim().toLowerCase()
  if (!keyword) {
    return commandItems.value
  }
  return commandItems.value.filter((item) => item.title.toLowerCase().includes(keyword))
})

watch(explorerCollapsed, (value) => {
  localStorage.setItem(explorerStorageKey, value ? 'collapsed' : 'open')
})

watch(inspectorCollapsed, (value) => {
  localStorage.setItem(inspectorStorageKey, value ? 'collapsed' : 'open')
})

watch(
  () => ({ ...shortcutDraft }),
  (value) => {
    localStorage.setItem(shortcutStorageKey, JSON.stringify(value))
  },
  { deep: true },
)

watch(boardCards, (value) => {
  localStorage.setItem(boardStorageKey, JSON.stringify(value))
}, { deep: true })

watch(
  () => route.params.slug,
  async (slug) => {
    if (route.name === 'notes' && typeof slug === 'string') {
      await store.bootstrap()
      if (isAuthenticated.value) {
        await store.selectNote(slug)
      }
    }
  },
  { immediate: true },
)

function toggleExplorer() {
  explorerCollapsed.value = !explorerCollapsed.value
}

function toggleInspector() {
  inspectorCollapsed.value = !inspectorCollapsed.value
}

function openCommandPalette() {
  commandOpen.value = true
  commandQuery.value = ''
}

function closeCommandPalette() {
  commandOpen.value = false
}

function openQuickIdeaCapture() {
  quickIdeaOpen.value = true
}

function closeQuickIdeaCapture() {
  quickIdeaOpen.value = false
}

function normalizeShortcut(shortcut: string) {
  return shortcut
    .replace(/\s+/g, '')
    .split('+')
    .filter(Boolean)
    .map((part) => part.toLowerCase())
}

function isShortcutMatch(event: KeyboardEvent, shortcut: string) {
  const parts = normalizeShortcut(shortcut)
  if (parts.length === 0) {
    return false
  }
  const key = event.key.length === 1 ? event.key.toLowerCase() : event.key.toLowerCase()
  return parts.every((part) => {
    if (part === 'ctrl') {
      return event.ctrlKey || event.metaKey
    }
    if (part === 'shift') {
      return event.shiftKey
    }
    if (part === 'alt') {
      return event.altKey
    }
    return key === part
  })
}

async function createTodayDailyNote() {
  const today = new Date().toISOString().slice(0, 10)
  const title = today
  const path = `05_Daily/${today}.md`
  const content = `# ${today}\n\n## 今日计划\n\n- \n\n## 记录\n\n`
  const slug = title.toLowerCase().replace(/[^\w\u4e00-\u9fff-]+/g, '-')
  const { data } = await apiClient.post('/notes', {
    title,
    slug,
    path,
    content,
    space_slug: store.currentSpaceSlug || store.currentUser?.default_space_slug || undefined,
    tags: ['daily'],
    links: [],
    source_url: '',
  })
  await store.refreshCollections(data.slug)
  await router.push(`/notes/${data.slug}`)
}

async function createFolderAtExplorer() {
  if (!createFolderPath.value.trim()) {
    return
  }
  await store.createFolder(createFolderPath.value.trim())
}

function addBoardCard() {
  boardCards.value = [
    ...boardCards.value,
    {
      id: crypto.randomUUID(),
      title: `卡片 ${boardCards.value.length + 1}`,
      body: '双击即可编辑内容。',
      x: 80 + boardCards.value.length * 36,
      y: 72 + boardCards.value.length * 30,
      color: ['#fff3c8', '#dff5dd', '#d9ebff', '#f3dcff'][boardCards.value.length % 4],
    },
  ]
}

function removeBoardCard(cardId: string) {
  boardCards.value = boardCards.value.filter((item) => item.id !== cardId)
}

async function createNoteFromTemplate(noteSlug: string) {
  const template = sortedNotes.value.find((item) => item.slug === noteSlug)
  if (!template) {
    return
  }
  const now = new Date().toISOString().slice(0, 16).replace('T', ' ')
  const title = `${template.title} ${now}`
  const slug = title.toLowerCase().replace(/[^\w\u4e00-\u9fff-]+/g, '-')
  const { data } = await apiClient.post('/notes', {
    title,
    slug,
    path: `01_Notes/${title}.md`,
    content: template.content,
    space_slug: store.currentSpaceSlug || store.currentUser?.default_space_slug || undefined,
    tags: [...template.tags],
    links: [],
    source_url: '',
  })
  await store.refreshCollections(data.slug)
  await router.push(`/notes/${data.slug}`)
}

function runCommand(commandId: string) {
  const command = commandItems.value.find((item) => item.id === commandId)
  if (!command) {
    return
  }
  closeCommandPalette()
  void command.run()
}

function handleKeyboard(event: KeyboardEvent) {
  if (isShortcutMatch(event, shortcutDraft.commandPalette)) {
    event.preventDefault()
    openCommandPalette()
    return
  }
  if (isShortcutMatch(event, shortcutDraft.toggleLeft)) {
    event.preventDefault()
    toggleExplorer()
    return
  }
  if (isShortcutMatch(event, shortcutDraft.toggleRight)) {
    event.preventDefault()
    toggleInspector()
    return
  }
  if (isShortcutMatch(event, shortcutDraft.newNote)) {
    event.preventDefault()
    void store.createQuickNote().then((slug) => router.push(`/notes/${slug}`))
    return
  }
  if (isShortcutMatch(event, shortcutDraft.dailyNote)) {
    event.preventDefault()
    void createTodayDailyNote()
    return
  }
  if (ideaPluginEnabled.value && isShortcutMatch(event, shortcutDraft.quickIdea)) {
    event.preventDefault()
    openQuickIdeaCapture()
  }
}

async function submitQuickIdea() {
  await store.captureQuickIdea()
  if (store.errorMessage) {
    return
  }
  closeQuickIdeaCapture()
}

function nextIdeaStatus(status: 'pending_review' | 'accepted' | 'planning' | 'building' | 'launched' | 'shelved') {
  if (status === 'pending_review') {
    return 'accepted'
  }
  if (status === 'accepted') {
    return 'planning'
  }
  if (status === 'planning') {
    return 'building'
  }
  if (status === 'building') {
    return 'launched'
  }
  if (status === 'shelved') {
    return 'accepted'
  }
  return 'planning'
}

function nextIdeaActionLabel(status: 'pending_review' | 'accepted' | 'planning' | 'building' | 'launched' | 'shelved') {
  if (status === 'pending_review') {
    return '接纳'
  }
  if (status === 'accepted') {
    return '进入规划'
  }
  if (status === 'planning') {
    return '进入实现'
  }
  if (status === 'building') {
    return '标记上线'
  }
  if (status === 'shelved') {
    return '重新启用'
  }
  return '回到规划'
}

onMounted(async () => {
  await store.bootstrap()
  window.addEventListener('keydown', handleKeyboard)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyboard)
})
</script>

<template>
  <main class="obsidian-shell" :class="{ 'obsidian-shell--explorer-collapsed': explorerCollapsed, 'obsidian-shell--inspector-collapsed': inspectorCollapsed }">
    <aside class="obsidian-rail">
      <button class="rail-button" type="button" title="命令面板" @click="openCommandPalette">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16" /><path d="M4 12h10" /><path d="M4 18h7" /></svg>
      </button>

      <button
        v-for="item in railItems"
        :key="item.id"
        class="rail-button"
        :class="{ 'rail-button--active': routeMode === item.id }"
        type="button"
        :title="item.label"
        @click="router.push(item.to)"
      >
        <svg v-if="item.id === 'home'" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 11.5 12 5l8 6.5" /><path d="M7 10.5V19h10v-8.5" /></svg>
        <svg v-else-if="item.id === 'topics'" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 5.5h12v4H6z" /><path d="M6 10.5h8v4H6z" /><path d="M6 15.5h10v4H6z" /><path d="M18 12.5h.01" /></svg>
        <svg v-else-if="item.id === 'ideas'" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4.5a5.5 5.5 0 0 1 3.8 9.5c-.9.8-1.3 1.5-1.3 2.5H9.5c0-1 .4-1.7-1.3-2.5A5.5 5.5 0 0 1 12 4.5z" /><path d="M9.5 18.5h5" /><path d="M10 21h4" /></svg>
        <svg v-else-if="item.id === 'passwords'" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 11a5 5 0 1 1 9.4 2.3H21v3h-2v2h-2v2h-3.2l-.7-.7v-3.3A5 5 0 0 1 7 11z" /><circle cx="9" cy="11" r="1.3" /></svg>
        <svg v-else-if="item.id === 'notes'" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4.5h8l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5z" /><path d="M15 4.5V9h4" /></svg>
        <svg v-else-if="item.id === 'daily'" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" /><path d="M8 2v4" /><path d="M16 2v4" /><path d="M5 9h14" /></svg>
        <svg v-else-if="item.id === 'templates'" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 5.5h14v13H5z" /><path d="M9 9h6" /><path d="M9 13h6" /><path d="M9 17h4" /></svg>
        <svg v-else-if="item.id === 'whiteboard'" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 4.5h16v11H4z" /><path d="M8 19h8" /><path d="M12 15.5V19" /></svg>
        <svg v-else-if="item.id === 'graph'" viewBox="0 0 24 24" aria-hidden="true"><circle cx="6.5" cy="7" r="2.5" /><circle cx="17.5" cy="6.5" r="2.5" /><circle cx="12" cy="17" r="3" /><path d="M8.8 8.2 10.5 14" /><path d="M15.2 8l-1.7 6" /><path d="M9 7h6" /></svg>
        <svg v-else-if="item.id === 'ai'" viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /><path d="m18.5 14 0.8 2.2 2.2 0.8-2.2 0.8-0.8 2.2-0.8-2.2-2.2-0.8 2.2-0.8z" /></svg>
        <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M10.3 2.8h3.4l.5 2.2a7.6 7.6 0 0 1 1.8.8l2-1.1 2.4 2.4-1.1 2a7.6 7.6 0 0 1 .8 1.8l2.2.5v3.4l-2.2.5a7.6 7.6 0 0 1-.8 1.8l1.1 2-2.4 2.4-2-1.1a7.6 7.6 0 0 1-1.8.8l-.5 2.2h-3.4l-.5-2.2a7.6 7.6 0 0 1-1.8-.8l-2 1.1-2.4-2.4 1.1-2a7.6 7.6 0 0 1-.8-1.8l-2.2-.5v-3.4l2.2-.5a7.6 7.6 0 0 1 .8-1.8l-1.1-2 2.4-2.4 2 1.1a7.6 7.6 0 0 1 1.8-.8z" /><circle cx="12" cy="12" r="3.1" /></svg>
      </button>
    </aside>

    <aside class="obsidian-explorer surface-panel">
      <header class="pane-header">
        <div>
          <p class="pane-eyebrow">Explorer</p>
          <h2>知识库</h2>
        </div>
        <div class="pane-actions pane-actions--compact">
          <button class="icon-button" type="button" title="折叠目录" @click="toggleExplorer">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 6 9 12l6 6" /></svg>
          </button>
        </div>
      </header>

      <div class="explorer-actions">
        <button class="icon-button" type="button" title="新建笔记" @click="store.createQuickNote().then((slug) => router.push(`/notes/${slug}`))">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg>
        </button>
        <input v-model="createFolderPath" class="field-input field-input--compact" placeholder="例如 01_Notes/项目/会议" />
        <button class="icon-button" type="button" title="新建文件夹" @click="createFolderAtExplorer">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H10l1.6 2H18.5A2.5 2.5 0 0 1 21 9.5v7A2.5 2.5 0 0 1 18.5 19h-13A2.5 2.5 0 0 1 3 16.5z" /><path d="M12 10v6" /><path d="M9 13h6" /></svg>
        </button>
      </div>

      <div class="explorer-tree">
        <TreeNode v-for="node in fileTree" :key="node.path" :node="node" :active-slug="currentNote?.slug" @select="(slug) => router.push(`/notes/${slug}`)" />
      </div>
    </aside>

    <section class="obsidian-main surface-panel">
      <header class="workspace-header">
        <div>
          <p class="pane-eyebrow">{{ railItems.find((item) => item.id === routeMode)?.label }}</p>
          <h1>{{ workspaceTitle }}</h1>
          <span class="helper-text">{{ workspaceSubtitle }}</span>
        </div>
        <div class="pane-actions">
          <button class="icon-button" type="button" title="命令面板" @click="openCommandPalette"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16" /><path d="M4 12h10" /><path d="M4 18h7" /></svg></button>
          <button class="icon-button" type="button" title="折叠右栏" @click="toggleInspector"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" /></svg></button>
        </div>
      </header>

      <section v-if="routeMode === 'home'" class="workspace-screen home-screen">
        <section class="content-card hero-card">
          <div>
            <p class="pane-eyebrow">Goal Alignment</p>
            <h3>{{ activeGoal?.title || '先定义一个核心目标，让首页成为你的每日对齐入口。' }}</h3>
            <p class="helper-text helper-text--wide">{{ activeGoal?.vision || '目标计划模块固定展示在首页顶部，先看目标、阶段计划与今日任务，再继续搜索、剪藏和创作。' }}</p>
          </div>
          <div class="stat-grid">
            <article class="mini-stat"><span>目标进度</span><strong>{{ activeGoal?.progress_percent ?? 0 }}%</strong></article>
            <article class="mini-stat"><span>今日任务</span><strong>{{ goalOverview.today_tasks.length }}</strong></article>
            <article class="mini-stat"><span>同步事件</span><strong>{{ syncStatus.last_event_id ?? 0 }}</strong></article>
          </div>
        </section>

        <div class="two-column-grid">
          <section class="content-card">
            <header class="panel-topbar">
              <h3>阶段计划</h3>
              <button class="icon-button" type="button" title="AI 拆解" @click="store.generateGoalPlanDraft"><svg viewBox="0 0 24 24"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /></svg></button>
            </header>
            <div class="stack-list">
              <article v-for="plan in activeGoalPlans" :key="plan.id" class="stack-item stack-item--static">
                <strong>{{ plan.title }}</strong>
                <span>{{ plan.progress_percent }}% · {{ plan.summary }}</span>
              </article>
              <article v-for="(draft, index) in goalAiPlanDraft" :key="`${draft.title}-${index}`" class="stack-item stack-item--static">
                <strong>{{ draft.title }}</strong>
                <span>{{ draft.summary }}</span>
                <p>{{ draft.tasks.join(' / ') }}</p>
                <button class="text-button" type="button" @click="applyGoalDraft(index)">带入表单</button>
              </article>
            </div>
          </section>

          <section class="content-card">
            <header class="panel-topbar">
              <h3>今日任务与复盘</h3>
              <button class="icon-button" type="button" title="打开今日日记" @click="createTodayDailyNote"><svg viewBox="0 0 24 24"><path d="M7 4h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z" /><path d="M8 2v4" /><path d="M16 2v4" /><path d="M5 9h14" /></svg></button>
            </header>
            <div class="stack-list">
              <button v-for="task in goalOverview.today_tasks" :key="task.id" class="stack-item" type="button" @click="store.toggleGoalTask(task.id, task.status !== 'done')">
                <strong>{{ task.title }}</strong>
                <span>{{ task.status === 'done' ? '已完成' : '待推进' }} · {{ task.priority }}</span>
                <p>{{ task.details || '点击切换任务状态，并同步到今日日记。' }}</p>
              </button>
            </div>
            <textarea v-model="goalJournalForm.reflection" class="field-input field-input--textarea" placeholder="写下今天的复盘，保存后会写入今日日记。" />
            <button class="text-button text-button--primary" type="button" @click="store.saveGoalJournal">保存今日复盘</button>
          </section>
        </div>

        <div class="two-column-grid">
          <section class="content-card">
            <header class="panel-topbar">
              <h3>全局搜索</h3>
              <button class="icon-button" type="button" title="搜索" @click="store.performSearch"><svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6" /><path d="m20 20-4.2-4.2" /></svg></button>
            </header>
            <input v-model="searchQuery" class="field-input" placeholder="搜索标题、正文、标签或术语" @keyup.enter="store.performSearch" />
            <div class="stack-list">
              <button v-for="item in searchResults" :key="`${item.kind}-${item.note_slug || item.password_id}`" class="stack-item" type="button" @click="openSearchItem(item)">
                <strong>{{ item.title }}</strong>
                <span>{{ item.kind === 'password' ? `${item.path} · 密码条目` : item.path }}</span>
                <p>{{ item.snippet }}</p>
              </button>
            </div>
          </section>

          <section class="content-card">
            <header class="panel-topbar">
              <h3>快速剪藏</h3>
              <button class="icon-button" type="button" title="解析链接" @click="store.submitClip"><svg viewBox="0 0 24 24"><path d="M10 14 21 3" /><path d="M21 3h-6" /><path d="M21 3v6" /><path d="M14 10 3 21" /></svg></button>
            </header>
            <input v-model="clipUrl" class="field-input" placeholder="粘贴网页链接" />
            <label class="toggle-chip">
              <input v-model="clipSummarizeWithAi" type="checkbox" />
              <span>AI 提炼总结</span>
            </label>
            <textarea v-if="clipSummarizeWithAi" v-model="clipSummaryPrompt" class="field-input field-input--textarea" placeholder="输入提炼要求" />
            <div class="recent-strip">
              <button v-for="note in recentNotes" :key="note.slug" class="mini-note" type="button" @click="router.push(`/notes/${note.slug}`)">
                <strong>{{ note.title }}</strong>
                <span>{{ note.path }}</span>
              </button>
            </div>
          </section>
        </div>
      </section>

      <section v-else-if="routeMode === 'notes'" class="workspace-screen notes-screen">
        <div class="editor-toolbar">
          <button class="icon-button" type="button" title="保存" @click="store.saveCurrentNote"><svg viewBox="0 0 24 24"><path d="M5 4.5h12l2 2V19H5z" /><path d="M8 4.5V9h8V4.5" /><path d="M9 19v-6h6v6" /></svg></button>
          <button class="icon-button" type="button" title="新建笔记" @click="store.createQuickNote().then((slug) => router.push(`/notes/${slug}`))"><svg viewBox="0 0 24 24"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
          <button class="icon-button" type="button" title="移动当前笔记" @click="rightTab = 'properties'; router.push('/settings')"><svg viewBox="0 0 24 24"><path d="M7 8 3 12l4 4" /><path d="M17 8l4 4-4 4" /><path d="M4 12h16" /></svg></button>
          <span class="status-pill">{{ saveState }}</span>
        </div>

        <input
          :value="currentNote?.title ?? ''"
          class="title-field"
          placeholder="笔记标题"
          @input="store.updateCurrentTitle(($event.target as HTMLInputElement).value); store.markDirty()"
        />
        <textarea
          :value="currentNote?.content ?? ''"
          class="markdown-editor"
          placeholder="在这里输入 Markdown 内容。"
          @input="store.updateCurrentContent(($event.target as HTMLTextAreaElement).value); store.markDirty()"
          @blur="store.saveCurrentNote"
        />
      </section>

      <section v-else-if="routeMode === 'passwords'" class="workspace-screen passwords-screen">
        <div class="two-column-grid">
          <section class="content-card">
            <header class="panel-topbar">
              <h3>密码库</h3>
              <button class="icon-button" type="button" title="生成强密码" @click="store.generatePassword"><svg viewBox="0 0 24 24"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
            </header>
            <p class="helper-text">{{ passwordVaultConfig.is_initialized ? '主密码只用于解锁与派生密钥，不作为明文保存。' : '首次使用需要先设置主密码。' }}</p>
            <input v-model="passwordForm.master_password" class="field-input" type="password" placeholder="输入主密码" />
            <div class="pane-actions">
              <button v-if="!passwordVaultConfig.is_initialized" class="text-button text-button--primary" type="button" @click="store.setupPasswordVault">初始化密码库</button>
              <button v-else class="text-button" type="button" @click="store.verifyPasswordVault">验证主密码</button>
              <span class="status-pill">{{ passwordVaultConfig.is_initialized ? passwordVerifyStatus : passwordSetupStatus }}</span>
            </div>
            <div class="stack-list">
              <button v-for="entry in passwordEntries" :key="entry.id" class="stack-item" type="button" @click="store.selectPasswordEntry(entry.id); store.populatePasswordForm(entry.id)">
                <strong>{{ entry.title }}</strong>
                <span>{{ entry.username || '未填账号' }} · {{ entry.category }}</span>
                <p>{{ entry.website || entry.url || '点击查看或编辑密码条目。' }}</p>
              </button>
            </div>
          </section>

          <section class="content-card">
            <header class="panel-topbar">
              <h3>{{ selectedPasswordEntry ? '编辑密码条目' : '新建密码条目' }}</h3>
              <button class="icon-button" type="button" title="清空表单" @click="store.populatePasswordForm()"><svg viewBox="0 0 24 24"><path d="m6 6 12 12" /><path d="M18 6 6 18" /></svg></button>
            </header>
            <div class="model-grid">
              <input v-model="passwordForm.title" class="field-input" placeholder="平台或账号名称" />
              <input v-model="passwordForm.username" class="field-input" placeholder="账号 / 邮箱 / 手机号" />
              <input v-model="passwordForm.password" class="field-input" type="text" placeholder="密码" />
              <select v-model="passwordForm.category" class="field-input">
                <option value="general">通用</option>
                <option value="work">工作</option>
                <option value="social">社交</option>
                <option value="finance">金融</option>
              </select>
              <input v-model="passwordForm.website" class="field-input" placeholder="网站或应用名称" />
              <input v-model="passwordForm.url" class="field-input" placeholder="登录网址" />
              <select v-model="passwordForm.vault_scope" class="field-input">
                <option value="private">仅自己</option>
                <option value="team">团队可见</option>
                <option value="selected">指定成员</option>
              </select>
              <input v-model="passwordGeneratorForm.length" class="field-input" type="number" min="12" max="64" placeholder="生成长度" />
            </div>
            <textarea v-model="passwordForm.notes" class="field-input field-input--textarea" placeholder="备注，例如 2FA、恢复流程、用途说明" />
            <div class="pane-actions">
              <button class="text-button" type="button" @click="store.generatePassword">生成强密码</button>
              <button class="text-button" type="button" :disabled="!selectedPasswordEntry" @click="selectedPasswordEntry && store.revealPassword(selectedPasswordEntry.id)">查看明文</button>
              <button class="text-button text-button--primary" type="button" @click="store.savePasswordEntry(selectedPasswordEntry?.id)">保存条目</button>
              <button v-if="selectedPasswordEntry" class="text-button" type="button" @click="store.deletePasswordEntry(selectedPasswordEntry.id)">删除</button>
            </div>
            <div class="inline-action-card">
              <div class="inline-action-card__header">
                <strong>解锁结果</strong>
                <button v-if="selectedPasswordEntry && revealedPassword" class="text-button" type="button" @click="copyRevealedPassword(selectedPasswordEntry.id)">复制密码</button>
              </div>
              <div class="property-list">
                <div class="property-row"><span>明文密码</span><strong>{{ revealedPassword || '尚未解锁' }}</strong></div>
                <div class="property-row"><span>最近使用</span><strong>{{ selectedPasswordEntry?.last_used_at || '-' }}</strong></div>
                <div class="property-row"><span>关联笔记</span><strong>{{ selectedPasswordEntry?.linked_note_slug || '-' }}</strong></div>
              </div>
            </div>
            <div class="stack-list">
              <article v-for="audit in passwordAudits" :key="audit.id" class="stack-item stack-item--static">
                <strong>{{ audit.action }}</strong>
                <span>{{ audit.created_at }}</span>
                <p>{{ audit.detail || '密码操作记录' }}</p>
              </article>
            </div>
          </section>
        </div>
      </section>

      <section v-else-if="routeMode === 'topics'" class="workspace-screen topics-screen">
        <template v-if="topicPluginEnabled">
          <section class="content-card hero-card">
            <div>
              <p class="pane-eyebrow">Topic Board</p>
              <h3>内容创作选题管理</h3>
              <p class="helper-text helper-text--wide">选题池和知识库、密码条目直接联动。你可以先用 AI 拉一批候选，再决定是否入池、补大纲和进入创作。</p>
            </div>
            <div class="stat-grid">
              <article class="mini-stat"><span>总选题</span><strong>{{ topicOverview.stats.total }}</strong></article>
              <article class="mini-stat"><span>完成率</span><strong>{{ topicOverview.stats.completion_rate }}%</strong></article>
              <article class="mini-stat"><span>高优先级</span><strong>{{ topicOverview.stats.high_priority }}</strong></article>
            </div>
          </section>

          <div class="two-column-grid">
            <section class="content-card">
              <header class="panel-topbar">
                <h3>AI 选题发现</h3>
                <button class="icon-button" type="button" title="生成选题" @click="store.discoverTopics"><svg viewBox="0 0 24 24"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /></svg></button>
              </header>
              <div class="model-grid">
                <input v-model="topicAiForm.domain" class="field-input" placeholder="领域，例如 AI 编程、私域运营" />
                <input v-model="topicAiForm.count" class="field-input" type="number" min="1" max="10" placeholder="生成数量" />
              </div>
              <textarea v-model="topicAiForm.keywords" class="field-input field-input--textarea" placeholder="每行一个关键词，例如：双向链接&#10;团队知识库&#10;效率工具" />
              <div class="pane-actions">
                <label class="toggle-chip">
                  <input v-model="topicAiForm.save_to_pool" type="checkbox" />
                  <span>直接写入选题池</span>
                </label>
                <span class="status-pill">{{ topicAiStatus }}</span>
              </div>
              <div class="stack-list">
                <article v-for="draft in topicDrafts" :key="`${draft.title}-${draft.created_at}`" class="stack-item stack-item--static">
                  <strong>{{ draft.title }}</strong>
                  <span>{{ draft.priority }} · 热度 {{ draft.heat_score }}</span>
                  <p>{{ draft.trend_summary }}</p>
                </article>
              </div>
            </section>

            <section class="content-card">
              <header class="panel-topbar">
                <h3>{{ selectedTopic ? '编辑选题' : '新建选题' }}</h3>
                <button class="icon-button" type="button" title="清空表单" @click="store.populateTopicForm()"><svg viewBox="0 0 24 24"><path d="m6 6 12 12" /><path d="M18 6 6 18" /></svg></button>
              </header>
              <div class="model-grid">
                <input v-model="topicForm.title" class="field-input" placeholder="选题标题" />
                <input v-model="topicForm.domain" class="field-input" placeholder="所属领域" />
                <select v-model="topicForm.status" class="field-input">
                  <option value="writable">可写选题</option>
                  <option value="pending">待创作</option>
                  <option value="in_progress">创作中</option>
                  <option value="completed">已完成</option>
                  <option value="shelved">已搁置</option>
                </select>
                <select v-model="topicForm.priority" class="field-input">
                  <option value="low">低优先级</option>
                  <option value="medium">中优先级</option>
                  <option value="high">高优先级</option>
                </select>
                <input v-model="topicForm.heat_score" class="field-input" type="number" min="0" max="100" placeholder="热度" />
                <input v-model="topicForm.due_date" class="field-input" type="date" />
              </div>
              <textarea v-model="topicForm.keywords" class="field-input field-input--textarea" placeholder="每行一个关键词" />
              <textarea v-model="topicForm.trend_summary" class="field-input field-input--textarea" placeholder="热点来源、趋势判断、适合切入的角度" />
              <textarea v-model="topicForm.ai_outline" class="field-input field-input--textarea" placeholder="AI 生成的大纲会显示在这里" />
              <div class="model-grid">
                <select v-model="topicForm.linked_note_slug" class="field-input">
                  <option value="">关联参考笔记</option>
                  <option v-for="note in sortedNotes" :key="`topic-note-${note.slug}`" :value="note.slug">{{ note.title }}</option>
                </select>
                <select v-model="topicForm.completed_note_slug" class="field-input">
                  <option value="">绑定成品笔记</option>
                  <option v-for="note in sortedNotes" :key="`topic-output-${note.slug}`" :value="note.slug">{{ note.title }}</option>
                </select>
              </div>
              <textarea v-model="topicForm.notes" class="field-input field-input--textarea" placeholder="补充备注、协作说明、写作约束" />
              <div class="pane-actions">
                <button class="text-button" type="button" @click="store.generateTopicOutline">AI 大纲</button>
                <button class="text-button text-button--primary" type="button" @click="store.saveTopic(selectedTopic?.id)">保存选题</button>
                <button v-if="selectedTopic" class="text-button" type="button" @click="store.deleteTopic(selectedTopic.id)">删除</button>
                <span class="status-pill">{{ topicSaveStatus }}</span>
              </div>
            </section>
          </div>

          <section class="content-card content-card--wide">
            <header class="panel-topbar">
              <h3>选题池</h3>
              <span class="status-pill">{{ filteredTopics.length }}</span>
            </header>
            <div class="stack-list">
              <article v-for="topic in filteredTopics" :key="topic.id" class="stack-item stack-item--static">
                <div class="stack-item__row">
                  <div>
                    <strong>{{ topic.title }}</strong>
                    <span>{{ topic.domain || '未分类' }} · {{ topic.status }} · {{ topic.priority }} · 热度 {{ topic.heat_score }}</span>
                    <p>{{ topic.trend_summary || topic.notes || '还没有补充说明。' }}</p>
                  </div>
                  <div class="pane-actions pane-actions--compact">
                    <button class="text-button" type="button" @click="store.populateTopicForm(topic.id)">编辑</button>
                    <button v-if="topic.linked_note_slug" class="text-button" type="button" @click="router.push(`/notes/${topic.linked_note_slug}`)">参考笔记</button>
                    <button v-if="topic.completed_note_slug" class="text-button" type="button" @click="router.push(`/notes/${topic.completed_note_slug}`)">成品笔记</button>
                    <button class="text-button" type="button" @click="store.updateTopicStatus(topic.id, topic.status === 'completed' ? 'writable' : 'completed')">{{ topic.status === 'completed' ? '重开' : '完成' }}</button>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <section class="content-card content-card--wide">
            <header class="panel-topbar">
              <h3>最近操作</h3>
            </header>
            <div class="stack-list">
              <article v-for="log in topicOverview.recent_logs" :key="log.id" class="stack-item stack-item--static">
                <strong>{{ log.action }}</strong>
                <span>#{{ log.topic_id }} · {{ log.created_at }}</span>
                <p>{{ log.detail || '选题操作记录' }}</p>
              </article>
            </div>
          </section>
        </template>

        <section v-else class="content-card content-card--wide">
          <header class="panel-topbar">
            <h3>选题插件未开启</h3>
            <button class="text-button text-button--primary" type="button" @click="router.push('/settings')">前往设置</button>
          </header>
          <p class="helper-text helper-text--wide">在设置页启用“内容选题管理”插件后，可以使用 AI 选题发现、状态流转、笔记/密码联动和创作大纲能力。</p>
        </section>
      </section>

      <section v-else-if="routeMode === 'ideas'" class="workspace-screen ideas-screen">
        <template v-if="ideaPluginEnabled">
          <section class="content-card hero-card">
            <div>
              <p class="pane-eyebrow">Ideas Board</p>
              <h3>创意点子与产品需求管理</h3>
              <p class="helper-text helper-text--wide">从灵感捕捉、价值评估到规划落地，统一沉淀在实例内，并和笔记、目标、选题形成闭环。</p>
            </div>
            <div class="stat-grid">
              <article class="mini-stat"><span>总条目</span><strong>{{ ideaOverview.stats.total }}</strong></article>
              <article class="mini-stat"><span>待评估</span><strong>{{ ideaOverview.stats.pending_review }}</strong></article>
              <article class="mini-stat"><span>平均机会分</span><strong>{{ ideaOverview.stats.average_opportunity_score }}</strong></article>
            </div>
          </section>

          <div class="two-column-grid">
            <section class="content-card">
              <header class="panel-topbar">
                <h3>快速捕捉</h3>
                <button class="icon-button" type="button" title="打开快速录入" @click="openQuickIdeaCapture"><svg viewBox="0 0 24 24"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
              </header>
              <div class="model-grid">
                <input v-model="quickIdeaForm.title" class="field-input" placeholder="灵感标题 / 用户痛点 / 需求机会" />
                <select v-model="quickIdeaForm.idea_type" class="field-input">
                  <option value="creative_idea">创意点子</option>
                  <option value="user_need">用户需求</option>
                  <option value="product_opportunity">产品机会</option>
                  <option value="optimization">优化建议</option>
                </select>
              </div>
              <textarea v-model="quickIdeaForm.summary" class="field-input field-input--textarea" placeholder="一句话描述触发背景、核心价值或待验证问题" />
              <div class="pane-actions">
                <select v-model="quickIdeaForm.priority" class="field-input field-input--compact">
                  <option value="low">低优先级</option>
                  <option value="medium">中优先级</option>
                  <option value="high">高优先级</option>
                </select>
                <button class="text-button text-button--primary" type="button" @click="submitQuickIdea">立即入池</button>
                <span class="status-pill">{{ ideaSaveStatus }}</span>
              </div>
              <div class="stack-list">
                <article v-for="idea in filteredIdeas.slice(0, 4)" :key="`idea-top-${idea.id}`" class="stack-item stack-item--static">
                  <strong>{{ idea.title }}</strong>
                  <span>{{ idea.idea_type }} · {{ idea.status }} · 机会分 {{ idea.opportunity_score }}</span>
                  <p>{{ idea.summary || idea.next_step || '等待补充细节。' }}</p>
                </article>
              </div>
            </section>

            <section class="content-card">
              <header class="panel-topbar">
                <h3>{{ selectedIdea ? '编辑条目' : '新建条目' }}</h3>
                <button class="icon-button" type="button" title="清空表单" @click="store.populateIdeaForm()"><svg viewBox="0 0 24 24"><path d="m6 6 12 12" /><path d="M18 6 6 18" /></svg></button>
              </header>
              <div class="model-grid">
                <input v-model="ideaForm.title" class="field-input" placeholder="标题" />
                <select v-model="ideaForm.idea_type" class="field-input">
                  <option value="creative_idea">创意点子</option>
                  <option value="user_need">用户需求</option>
                  <option value="product_opportunity">产品机会</option>
                  <option value="optimization">优化建议</option>
                </select>
                <select v-model="ideaForm.status" class="field-input">
                  <option value="pending_review">待评估</option>
                  <option value="accepted">已接纳</option>
                  <option value="planning">规划中</option>
                  <option value="building">实现中</option>
                  <option value="launched">已上线</option>
                  <option value="shelved">已搁置</option>
                </select>
                <select v-model="ideaForm.priority" class="field-input">
                  <option value="low">低优先级</option>
                  <option value="medium">中优先级</option>
                  <option value="high">高优先级</option>
                </select>
              </div>
              <textarea v-model="ideaForm.summary" class="field-input field-input--textarea" placeholder="价值摘要" />
              <textarea v-model="ideaForm.details" class="field-input field-input--textarea" placeholder="补充完整背景、用户场景、验收口径、实现思路" />
              <div class="model-grid">
                <input v-model="ideaForm.value_score" class="field-input" type="number" min="0" max="100" placeholder="价值度" />
                <input v-model="ideaForm.effort_score" class="field-input" type="number" min="0" max="100" placeholder="实现成本" />
                <input v-model="ideaForm.business_score" class="field-input" type="number" min="0" max="100" placeholder="商业潜力" />
                <input v-model="ideaForm.next_step" class="field-input" placeholder="下一步动作" />
              </div>
              <div class="model-grid">
                <select v-model="ideaForm.linked_note_slug" class="field-input">
                  <option value="">关联参考笔记</option>
                  <option v-for="note in sortedNotes" :key="`idea-note-${note.slug}`" :value="note.slug">{{ note.title }}</option>
                </select>
                <select v-model="ideaForm.linked_goal_id" class="field-input">
                  <option :value="null">关联目标</option>
                  <option v-for="goal in goalOverview.goals" :key="`idea-goal-${goal.id}`" :value="goal.id">{{ goal.title }}</option>
                </select>
                <select v-model="ideaForm.linked_topic_id" class="field-input">
                  <option :value="null">关联选题</option>
                  <option v-for="topic in topicOverview.topics" :key="`idea-topic-${topic.id}`" :value="topic.id">{{ topic.title }}</option>
                </select>
                <select v-model="ideaForm.visibility_scope" class="field-input">
                  <option value="private">仅自己可见</option>
                  <option value="team">实例内共享</option>
                </select>
              </div>
              <textarea v-model="ideaForm.tags" class="field-input field-input--textarea" placeholder="每行一个标签，例如：增长&#10;团队协作&#10;商业化" />
              <textarea v-model="ideaForm.source_context" class="field-input field-input--textarea" placeholder="记录触发来源，例如聊天片段、用户反馈、阅读摘录" />
              <div class="pane-actions">
                <button class="text-button text-button--primary" type="button" @click="store.saveIdea(selectedIdea?.id)">保存条目</button>
                <button v-if="selectedIdea" class="text-button" type="button" @click="store.deleteIdea(selectedIdea.id)">删除</button>
                <span class="status-pill">{{ ideaSaveStatus }}</span>
              </div>
            </section>
          </div>

          <section class="content-card content-card--wide">
            <header class="panel-topbar">
              <h3>需求池</h3>
              <span class="status-pill">{{ filteredIdeas.length }}</span>
            </header>
            <div class="stack-list">
              <article v-for="idea in filteredIdeas" :key="idea.id" class="stack-item stack-item--static">
                <div class="stack-item__row">
                  <div>
                    <strong>{{ idea.title }}</strong>
                    <span>{{ idea.idea_type }} · {{ idea.status }} · {{ idea.priority }} · 机会分 {{ idea.opportunity_score }}</span>
                    <p>{{ idea.summary || idea.details || '尚未补充描述。' }}</p>
                  </div>
                  <div class="pane-actions pane-actions--compact">
                    <button class="text-button" type="button" @click="store.populateIdeaForm(idea.id)">编辑</button>
                    <button v-if="idea.linked_note_slug" class="text-button" type="button" @click="router.push(`/notes/${idea.linked_note_slug}`)">关联笔记</button>
                    <button class="text-button" type="button" @click="store.updateIdeaStatus(idea.id, nextIdeaStatus(idea.status))">{{ nextIdeaActionLabel(idea.status) }}</button>
                    <button v-if="idea.status !== 'shelved'" class="text-button" type="button" @click="store.updateIdeaStatus(idea.id, 'shelved')">搁置</button>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <section class="content-card content-card--wide">
            <header class="panel-topbar">
              <h3>最近操作</h3>
            </header>
            <div class="stack-list">
              <article v-for="log in ideaOverview.recent_logs" :key="log.id" class="stack-item stack-item--static">
                <strong>{{ log.action }}</strong>
                <span>#{{ log.idea_id }} · {{ log.created_at }}</span>
                <p>{{ log.detail || '创意需求操作记录' }}</p>
              </article>
            </div>
          </section>
        </template>

        <section v-else class="content-card content-card--wide">
          <header class="panel-topbar">
            <h3>创意插件未开启</h3>
            <button class="text-button text-button--primary" type="button" @click="router.push('/settings')">前往设置</button>
          </header>
          <p class="helper-text helper-text--wide">在设置页启用“创意点子与需求管理”插件后，可以用快速弹窗录入、状态闭环、价值评估和知识库联动统一管理灵感与需求。</p>
        </section>
      </section>

      <section v-else-if="routeMode === 'graph'" class="workspace-screen graph-screen">
        <GraphMapPanel />
      </section>

      <section v-else-if="routeMode === 'ai'" class="workspace-screen ai-screen">
        <div class="two-column-grid">
          <section class="content-card">
            <header class="panel-topbar"><h3>创作面板</h3></header>
            <textarea v-model="aiPrompt" class="field-input field-input--textarea field-input--tall" placeholder="输入总结、扩写、润色、问答或创作要求" />
            <div class="icon-action-grid">
              <button class="icon-button icon-button--labeled" type="button" @click="store.runAi('summary')"><svg viewBox="0 0 24 24"><path d="M6 7h12" /><path d="M6 12h8" /><path d="M6 17h10" /></svg><span>总结</span></button>
              <button class="icon-button icon-button--labeled" type="button" @click="store.runAi('expand')"><svg viewBox="0 0 24 24"><path d="M12 5v14" /><path d="M5 12h14" /></svg><span>扩写</span></button>
              <button class="icon-button icon-button--labeled" type="button" @click="store.runAi('polish')"><svg viewBox="0 0 24 24"><path d="M4 17.5V20h2.5L18 8.5 15.5 6z" /><path d="M13.5 8 16 10.5" /></svg><span>润色</span></button>
              <button class="icon-button icon-button--labeled" type="button" @click="store.runAi('qa')"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="M9.5 9.8a2.5 2.5 0 0 1 5 0c0 1.6-2.5 2-2.5 4" /><path d="M12 17h.01" /></svg><span>问答</span></button>
              <button class="icon-button icon-button--labeled icon-button--primary" type="button" @click="store.runAi('create', true)"><svg viewBox="0 0 24 24"><path d="m12 3 1.6 4.3L18 9l-4.4 1.7L12 15l-1.6-4.3L6 9l4.4-1.7z" /></svg><span>创作保存</span></button>
            </div>
          </section>
          <section class="content-card">
            <header class="panel-topbar"><h3>生成结果</h3></header>
            <pre class="output-panel">{{ aiResponse || '结果会显示在这里。' }}</pre>
            <button v-if="showAiPreview && aiResponse" class="text-button" type="button" @click="store.applyAiResultToCurrentNote">追加到当前笔记</button>
            <div class="stack-list">
              <article v-for="item in aiReferences" :key="`${item.note_slug}-${item.score}`" class="stack-item stack-item--static">
                <strong>{{ item.title }}</strong>
                <span>{{ item.score }}</span>
                <p>{{ item.snippet }}</p>
              </article>
            </div>
          </section>
        </div>
      </section>

      <section v-else-if="routeMode === 'whiteboard'" class="workspace-screen whiteboard-screen">
        <header class="panel-topbar panel-topbar--spaced">
          <h3>白板</h3>
          <button class="icon-button" type="button" title="添加卡片" @click="addBoardCard"><svg viewBox="0 0 24 24"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
        </header>
        <div class="whiteboard-canvas">
          <article v-for="card in boardCards" :key="card.id" class="board-card" :style="{ left: `${card.x}px`, top: `${card.y}px`, background: card.color }">
            <input v-model="card.title" class="board-card__title" />
            <textarea v-model="card.body" class="board-card__body"></textarea>
            <button class="board-card__remove" type="button" @click="removeBoardCard(card.id)">×</button>
          </article>
          <div v-if="boardCards.length === 0" class="whiteboard-empty">点击右上角添加卡片，开始组织你的想法。</div>
        </div>
      </section>

      <section v-else-if="routeMode === 'daily'" class="workspace-screen daily-screen">
        <header class="panel-topbar panel-topbar--spaced">
          <h3>日记</h3>
          <button class="icon-button" type="button" title="创建今日日记" @click="createTodayDailyNote"><svg viewBox="0 0 24 24"><path d="M12 5v14" /><path d="M5 12h14" /></svg></button>
        </header>
        <section class="content-card">
          <header class="panel-topbar"><h3>目标计划录入</h3></header>
          <div class="model-grid">
            <input v-model="goalForm.title" class="field-input" placeholder="核心目标" />
            <select v-model="goalForm.priority" class="field-input">
              <option value="low">低优先级</option>
              <option value="medium">中优先级</option>
              <option value="high">高优先级</option>
            </select>
            <input v-model="goalPlanForm.title" class="field-input" placeholder="阶段计划标题" />
            <input v-model="goalTaskForm.title" class="field-input" placeholder="今日任务标题" />
          </div>
          <textarea v-model="goalForm.vision" class="field-input field-input--textarea" placeholder="写下目标愿景与约束。" />
          <textarea v-model="goalForm.key_results" class="field-input field-input--textarea" placeholder="每行一个关键结果。" />
          <div class="pane-actions">
            <button class="text-button" type="button" @click="store.generateGoalPlanDraft">AI 拆解</button>
            <button class="text-button text-button--primary" type="button" @click="store.createGoal">保存目标</button>
            <button class="text-button" type="button" @click="store.createGoalPlan">保存阶段</button>
            <button class="text-button" type="button" @click="store.createGoalTask">保存任务</button>
          </div>
        </section>
        <div class="stack-list">
          <button v-for="note in dailyNotes" :key="note.slug" class="stack-item" type="button" @click="router.push(`/notes/${note.slug}`)">
            <strong>{{ note.title }}</strong>
            <span>{{ note.path }}</span>
            <p>{{ note.summary || '打开继续记录今天的内容。' }}</p>
          </button>
        </div>
      </section>

      <section v-else-if="routeMode === 'templates'" class="workspace-screen templates-screen">
        <header class="panel-topbar panel-topbar--spaced">
          <h3>模板</h3>
          <button class="icon-button" type="button" title="打开模板目录" @click="router.push('/notes')"><svg viewBox="0 0 24 24"><path d="M7 4.5h8l4 4V19H5V6A1.5 1.5 0 0 1 6.5 4.5z" /><path d="M15 4.5V9h4" /></svg></button>
        </header>
        <div class="stack-list">
          <article v-for="note in templateNotes" :key="note.slug" class="stack-item stack-item--static">
            <strong>{{ note.title }}</strong>
            <span>{{ note.path }}</span>
            <p>{{ note.summary || '使用这个模板快速新建内容。' }}</p>
            <button class="text-button" type="button" @click="createNoteFromTemplate(note.slug)">基于模板新建</button>
          </article>
        </div>
      </section>

      <section v-else class="workspace-screen settings-screen">
        <div class="two-column-grid">
          <section class="content-card">
            <header class="panel-topbar"><h3>外观与布局</h3></header>
            <div class="settings-list">
              <label class="setting-row">
                <span>左侧目录</span>
                <button class="text-button" type="button" @click="toggleExplorer">{{ explorerCollapsed ? '展开' : '折叠' }}</button>
              </label>
              <label class="setting-row">
                <span>右侧扩展栏</span>
                <button class="text-button" type="button" @click="toggleInspector">{{ inspectorCollapsed ? '展开' : '折叠' }}</button>
              </label>
              <label class="setting-row">
                <span>命令面板</span>
                <button class="text-button" type="button" @click="openCommandPalette">打开</button>
              </label>
            </div>
          </section>

          <section class="content-card">
            <header class="panel-topbar"><h3>快捷键</h3></header>
            <div class="settings-list">
              <label class="setting-row setting-row--field"><span>命令面板</span><input v-model="shortcutDraft.commandPalette" class="field-input field-input--compact" /></label>
              <label class="setting-row setting-row--field"><span>折叠左栏</span><input v-model="shortcutDraft.toggleLeft" class="field-input field-input--compact" /></label>
              <label class="setting-row setting-row--field"><span>折叠右栏</span><input v-model="shortcutDraft.toggleRight" class="field-input field-input--compact" /></label>
              <label class="setting-row setting-row--field"><span>新建笔记</span><input v-model="shortcutDraft.newNote" class="field-input field-input--compact" /></label>
              <label class="setting-row setting-row--field"><span>今日日记</span><input v-model="shortcutDraft.dailyNote" class="field-input field-input--compact" /></label>
              <label class="setting-row setting-row--field"><span>快速记录创意</span><input v-model="shortcutDraft.quickIdea" class="field-input field-input--compact" /></label>
            </div>
          </section>

          <section class="content-card">
            <header class="panel-topbar"><h3>系统插件</h3></header>
            <div class="settings-list">
              <label class="setting-row">
                <span>内容选题管理</span>
                <button class="text-button" type="button" @click="store.togglePlugin('topics', !topicPluginEnabled)">{{ topicPluginEnabled ? '关闭' : '开启' }}</button>
              </label>
              <p class="helper-text helper-text--wide">开启后会显示独立选题工作台，支持 AI 发现选题、状态管理、笔记/密码联动和团队协作扩展字段。</p>
              <label class="setting-row">
                <span>创意点子与需求管理</span>
                <button class="text-button" type="button" @click="store.togglePlugin('ideas', !ideaPluginEnabled)">{{ ideaPluginEnabled ? '关闭' : '开启' }}</button>
              </label>
              <p class="helper-text helper-text--wide">开启后会显示独立需求池工作台，支持全局快速录入、价值/成本/商业潜力评估、状态闭环与知识库联动。</p>
            </div>
          </section>

          <section class="content-card content-card--wide">
            <header class="panel-topbar"><h3>模型配置</h3></header>
            <div class="model-grid">
              <select v-model="aiProviderForm.provider_id" class="field-input" @change="store.applyAiProviderPreset(aiProviderForm.provider_id)">
                <option v-for="preset in aiProviderPresets" :key="preset.provider_id" :value="preset.provider_id">{{ preset.label }}</option>
              </select>
              <input v-model="aiProviderForm.provider_label" class="field-input" placeholder="显示名称" />
              <input v-model="aiProviderForm.base_url" class="field-input" placeholder="Base URL" />
              <input v-model="aiProviderForm.model_name" class="field-input" placeholder="模型名称" />
              <input v-model="aiProviderForm.api_key" class="field-input" type="password" placeholder="新的 API Key" />
              <button class="text-button text-button--primary" type="button" @click="store.saveAiProviderConfig">保存配置</button>
            </div>
            <p class="helper-text">当前模型：{{ aiProviderCurrent?.provider_label || '未配置' }} {{ aiProviderCurrent?.model_name || '' }}</p>
          </section>
        </div>
      </section>
    </section>

    <aside class="obsidian-inspector surface-panel">
      <header class="pane-header">
        <div>
          <p class="pane-eyebrow">Inspector</p>
          <h2>扩展栏</h2>
        </div>
        <div class="pane-actions pane-actions--compact">
          <button class="icon-button" type="button" title="折叠右栏" @click="toggleInspector"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6l6 6-6 6" /></svg></button>
        </div>
      </header>

      <div class="inspector-tabs">
        <button class="tab-button" :class="{ 'tab-button--active': rightTab === 'outline' }" type="button" @click="rightTab = 'outline'">目录</button>
        <button class="tab-button" :class="{ 'tab-button--active': rightTab === 'links' }" type="button" @click="rightTab = 'links'">链路</button>
        <button class="tab-button" :class="{ 'tab-button--active': rightTab === 'tags' }" type="button" @click="rightTab = 'tags'">标签</button>
        <button class="tab-button" :class="{ 'tab-button--active': rightTab === 'properties' }" type="button" @click="rightTab = 'properties'">属性</button>
      </div>

      <section v-if="rightTab === 'outline'" class="inspector-section">
        <button v-for="item in noteOutline" :key="`${item.level}-${item.text}`" class="outline-item" type="button">
          <span :style="{ paddingLeft: `${(item.level - 1) * 14}px` }">{{ item.text }}</span>
        </button>
      </section>

      <section v-else-if="rightTab === 'links'" class="inspector-section">
        <div class="inspector-block">
          <strong>出链</strong>
          <button v-for="item in backlinks.outgoing" :key="`${item.source_slug}-${item.target_slug}`" class="outline-item" type="button" @click="router.push(`/notes/${item.target_slug}`)">
            [[{{ item.target_slug }}]]
          </button>
        </div>
        <div class="inspector-block">
          <strong>反链</strong>
          <button v-for="item in backlinks.incoming" :key="`${item.source_slug}-${item.target_slug}`" class="outline-item" type="button" @click="router.push(`/notes/${item.source_slug}`)">
            {{ item.source_slug }}
          </button>
        </div>
      </section>

      <section v-else-if="rightTab === 'tags'" class="inspector-section">
        <div class="tag-cloud">
          <button v-for="tag in currentNote?.tags || []" :key="tag" class="tag-pill" type="button">#{{ tag }}</button>
          <button v-for="tag in tags.slice(0, 12)" :key="tag.tag" class="tag-pill tag-pill--muted" type="button">#{{ tag.tag }} {{ tag.count }}</button>
        </div>
      </section>

      <section v-else class="inspector-section">
        <div class="property-list">
          <div class="property-row"><span>标题</span><strong>{{ currentNote?.title || '-' }}</strong></div>
          <div class="property-row"><span>路径</span><strong>{{ currentNote?.path || '-' }}</strong></div>
          <div class="property-row"><span>更新时间</span><strong>{{ currentNote?.updated_at || '-' }}</strong></div>
          <div class="property-row"><span>标签数</span><strong>{{ currentNote?.tags.length || 0 }}</strong></div>
          <div class="property-row"><span>同步队列</span><strong>{{ syncStatus.pending_events }}</strong></div>
          <div class="property-row"><span>密码条目</span><strong>{{ passwordEntries.length }}</strong></div>
          <div class="property-row"><span>活跃目标</span><strong>{{ goalOverview.goals.length }}</strong></div>
          <div class="property-row"><span>内容选题</span><strong>{{ topicOverview.stats.total }}</strong></div>
          <div class="property-row"><span>创意需求</span><strong>{{ ideaOverview.stats.total }}</strong></div>
        </div>
      </section>
    </aside>

    <div v-if="showClipPreview && clipPreview" class="modal-backdrop" @click.self="store.closeClipPreview">
      <div class="modal-card">
        <header class="panel-topbar panel-topbar--spaced">
          <h3>{{ clipPreview.extracted_title }}</h3>
          <button class="icon-button" type="button" @click="store.closeClipPreview"><svg viewBox="0 0 24 24"><path d="m6 6 12 12" /><path d="M18 6 6 18" /></svg></button>
        </header>
        <pre class="output-panel">{{ clipPreview.preview_content }}</pre>
        <div class="pane-actions pane-actions--right">
          <button class="text-button" type="button" @click="store.closeClipPreview">取消</button>
          <button class="text-button text-button--primary" type="button" :disabled="isBusy" @click="store.saveClipPreview">保存</button>
        </div>
      </div>
    </div>

    <button
      v-if="ideaPluginEnabled"
      class="quick-capture-fab"
      type="button"
      title="快速捕捉创意"
      @click="openQuickIdeaCapture"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14" /><path d="M5 12h14" /></svg>
      <span>记录点子</span>
    </button>

    <div v-if="quickIdeaOpen" class="modal-backdrop" @click.self="closeQuickIdeaCapture">
      <div class="modal-card">
        <header class="panel-topbar panel-topbar--spaced">
          <div>
            <p class="pane-eyebrow">Quick Capture</p>
            <h3>快速收录创意与需求</h3>
          </div>
          <button class="icon-button" type="button" @click="closeQuickIdeaCapture"><svg viewBox="0 0 24 24"><path d="m6 6 12 12" /><path d="M18 6 6 18" /></svg></button>
        </header>
        <div class="model-grid">
          <input v-model="quickIdeaForm.title" class="field-input" placeholder="标题，例如：AI 自动整理用户痛点" />
          <select v-model="quickIdeaForm.idea_type" class="field-input">
            <option value="creative_idea">创意点子</option>
            <option value="user_need">用户需求</option>
            <option value="product_opportunity">产品机会</option>
            <option value="optimization">优化建议</option>
          </select>
        </div>
        <textarea v-model="quickIdeaForm.summary" class="field-input field-input--textarea" placeholder="输入一句话背景、用户痛点或机会判断" />
        <div class="pane-actions pane-actions--right">
          <button class="text-button" type="button" @click="closeQuickIdeaCapture">取消</button>
          <button class="text-button text-button--primary" type="button" :disabled="isBusy" @click="submitQuickIdea">入池</button>
        </div>
      </div>
    </div>

    <div v-if="commandOpen" class="modal-backdrop" @click.self="closeCommandPalette">
      <div class="command-palette surface-panel">
        <input v-model="commandQuery" class="field-input" placeholder="输入命令名称，例如：新建笔记、折叠侧栏、白板" />
        <div class="command-list">
          <button v-for="item in visibleCommands" :key="item.id" class="command-item" type="button" @click="runCommand(item.id)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.shortcut }}</span>
          </button>
        </div>
      </div>
    </div>
  </main>
</template>