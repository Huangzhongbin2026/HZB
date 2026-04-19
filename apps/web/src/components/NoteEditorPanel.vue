<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

import { useWorkspaceStore } from '../stores/workspace'

const editorRef = ref<HTMLDivElement | null>(null)
const editor = ref<Vditor | null>(null)
const autosaveTimer = ref<number | null>(null)
const isMoveFormVisible = ref(false)
const moveTargetPath = ref('')
const store = useWorkspaceStore()
const router = useRouter()
const { currentNote, saveState, syncStatus } = storeToRefs(store)

const saveLabel = computed(() => {
  if (saveState.value === 'saving') {
    return '自动保存中'
  }
  if (saveState.value === 'saved') {
    return '已自动同步'
  }
  if (saveState.value === 'error') {
    return '同步失败'
  }
  return `${syncStatus.value.pending_events} 待同步`
})

function queueAutosave() {
  if (!currentNote.value) {
    return
  }
  if (autosaveTimer.value) {
    window.clearTimeout(autosaveTimer.value)
  }
  store.markDirty()
  autosaveTimer.value = window.setTimeout(() => {
    store.saveCurrentNote()
  }, 1200)
}

onMounted(() => {
  if (!editorRef.value) {
    return
  }

  editor.value = new Vditor(editorRef.value, {
    minHeight: 640,
    cache: { enable: false },
    toolbarConfig: { pin: true },
    value: currentNote.value?.content ?? '# 请选择笔记',
    placeholder: '输入 Markdown，自动保存会在停止输入后触发。',
    input(value) {
      store.updateCurrentContent(value)
      queueAutosave()
    },
  })
})

watch(
  () => currentNote.value?.content,
  (value) => {
    if (!editor.value || value === undefined) {
      return
    }
    if (editor.value.getValue() !== value) {
      editor.value.setValue(value)
    }
  },
)

onBeforeUnmount(() => {
  if (autosaveTimer.value) {
    window.clearTimeout(autosaveTimer.value)
  }
})

async function deleteCurrent() {
  const deletedSlug = currentNote.value?.slug
  await store.deleteCurrentNote()
  if (deletedSlug) {
    router.push('/notes')
  }
}

async function renameCurrentPath() {
  if (!currentNote.value) {
    return
  }
  moveTargetPath.value = currentNote.value.path
  isMoveFormVisible.value = true
}

function cancelMoveForm() {
  isMoveFormVisible.value = false
}

async function submitMoveForm() {
  if (!moveTargetPath.value.trim()) {
    return
  }
  await store.moveCurrentNote(moveTargetPath.value.trim())
  isMoveFormVisible.value = false
}
</script>

<template>
  <section class="surface-card editor-screen">
    <header class="surface-header surface-header--editor">
      <div>
        <input
          :value="currentNote?.title ?? ''"
          class="title-input"
          placeholder="笔记标题"
          @input="store.updateCurrentTitle(($event.target as HTMLInputElement).value); queueAutosave()"
        />
        <span class="note-path">{{ currentNote?.path ?? '从左侧目录选择一篇笔记进入独立编辑页' }}</span>
      </div>
      <div class="panel-actions panel-actions--icon">
        <span class="save-indicator" :class="`save-indicator--${saveState}`">{{ saveLabel }}</span>
        <button class="icon-button" type="button" title="重命名或移动" aria-label="重命名或移动" @click="renameCurrentPath">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 17.5V20h2.5L18 8.5 15.5 6z" />
            <path d="M13.5 8 16 10.5" />
          </svg>
        </button>
        <button class="icon-button icon-button--danger" type="button" title="删除笔记" aria-label="删除笔记" @click="deleteCurrent">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16" />
            <path d="M10 11v5" />
            <path d="M14 11v5" />
            <path d="M6.5 7 7.3 19a1 1 0 0 0 1 .9h7.4a1 1 0 0 0 1-.9L17.5 7" />
            <path d="M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7" />
          </svg>
        </button>
      </div>
    </header>

    <div v-if="isMoveFormVisible" class="inline-action-card inline-action-card--editor">
      <div class="inline-action-card__header">
        <strong>重命名或移动笔记</strong>
        <button class="ghost-button ghost-button--small" type="button" @click="cancelMoveForm">取消</button>
      </div>
      <div class="inline-action-grid">
        <label class="inline-field">
          <span>目标路径</span>
          <input v-model="moveTargetPath" class="field-input" placeholder="例如 01_Notes/system-overview.md" />
        </label>
        <button class="primary-button primary-button--small" type="button" @click="submitMoveForm">应用路径</button>
      </div>
    </div>

    <div ref="editorRef" class="editor-host editor-host--tall"></div>
  </section>
</template>
