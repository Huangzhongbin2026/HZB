<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()
const { activeTagFilter, currentNote, sortedNotes, tags } = storeToRefs(store)
const directoryAction = ref<'create-folder' | 'rename-folder' | 'move-note' | null>(null)
const folderPath = ref('')
const folderSourcePath = ref('')
const folderTargetPath = ref('')
const noteTargetPath = ref('')

const visibleTags = computed(() => tags.value.slice(0, 12))
const groupedNotes = computed(() => {
  const groups = new Map<string, typeof sortedNotes.value>()
  for (const note of sortedNotes.value) {
    const folder = note.parent_path || '未分类'
    const current = groups.get(folder) ?? []
    current.push(note)
    groups.set(folder, current)
  }
  return [...groups.entries()].map(([folder, notes]) => ({ folder, notes }))
})
const currentFolderPath = computed(() => {
  const path = currentNote.value?.path ?? ''
  const segments = path.split('/').filter(Boolean)
  segments.pop()
  return segments.join('/')
})

function setDirectoryAction(action: 'create-folder' | 'rename-folder' | 'move-note') {
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

function openNote(slug: string) {
  router.push(`/notes/${slug}`)
}

async function createAndOpen() {
  const slug = await store.createQuickNote()
  if (slug) {
    router.push(`/notes/${slug}`)
  }
}

async function submitCreateFolder() {
  if (!folderPath.value.trim()) {
    return
  }
  await store.createFolder(folderPath.value.trim())
  closeDirectoryAction()
}

async function submitRenameFolder() {
  if (!folderSourcePath.value.trim() || !folderTargetPath.value.trim()) {
    return
  }
  await store.renameFolder(folderSourcePath.value.trim(), folderTargetPath.value.trim())
  closeDirectoryAction()
}

async function submitMoveCurrentNote() {
  if (!store.currentNote || !noteTargetPath.value.trim()) {
    return
  }
  await store.moveCurrentNote(noteTargetPath.value.trim())
  closeDirectoryAction()
  router.push(`/notes/${store.currentNote.slug}`)
}
</script>

<template>
  <section class="surface-card note-directory-panel">
    <header class="surface-header">
      <div>
        <h2>笔记</h2>
      </div>
      <div class="panel-actions panel-actions--icon">
        <button class="icon-button" type="button" title="新建文件夹" aria-label="新建文件夹" @click="setDirectoryAction('create-folder')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H10l1.6 2H18.5A2.5 2.5 0 0 1 21 9.5v7A2.5 2.5 0 0 1 18.5 19h-13A2.5 2.5 0 0 1 3 16.5z" />
            <path d="M12 10v6" />
            <path d="M9 13h6" />
          </svg>
        </button>
        <button class="icon-button" type="button" title="重命名文件夹" aria-label="重命名文件夹" @click="setDirectoryAction('rename-folder')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 17.5V20h2.5L18 8.5 15.5 6z" />
            <path d="M13.5 8 16 10.5" />
            <path d="M4 4h10" />
          </svg>
        </button>
        <button class="icon-button" type="button" title="移动当前笔记" aria-label="移动当前笔记" @click="setDirectoryAction('move-note')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M7 8 3 12l4 4" />
            <path d="M17 8l4 4-4 4" />
            <path d="M4 12h16" />
          </svg>
        </button>
        <button class="icon-button icon-button--primary" type="button" title="新建笔记" aria-label="新建笔记" @click="createAndOpen">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M7 4.5h7l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5z" />
            <path d="M14 4.5V9h4.5" />
            <path d="M12 11v6" />
            <path d="M9 14h6" />
          </svg>
        </button>
      </div>
    </header>

    <div v-if="directoryAction" class="inline-action-card">
      <div class="inline-action-card__header">
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

      <div v-if="directoryAction === 'create-folder'" class="inline-action-grid">
        <label class="inline-field">
          <span>文件夹路径</span>
          <input v-model="folderPath" class="field-input" placeholder="例如 01_Notes/项目笔记" />
        </label>
        <button class="primary-button primary-button--small" type="button" @click="submitCreateFolder">确认创建</button>
      </div>

      <div v-else-if="directoryAction === 'rename-folder'" class="inline-action-grid inline-action-grid--double">
        <label class="inline-field">
          <span>当前路径</span>
          <input v-model="folderSourcePath" class="field-input" placeholder="例如 01_Notes/项目笔记" />
        </label>
        <label class="inline-field">
          <span>新路径</span>
          <input v-model="folderTargetPath" class="field-input" placeholder="例如 01_Notes/归档/项目笔记" />
        </label>
        <button class="primary-button primary-button--small" type="button" @click="submitRenameFolder">确认重命名</button>
      </div>

      <div v-else class="inline-action-grid">
        <label class="inline-field">
          <span>目标路径</span>
          <input v-model="noteTargetPath" class="field-input" placeholder="例如 02_Articles/system-overview.md" />
        </label>
        <span class="helper-text">当前笔记：{{ currentNote?.title ?? '未选中' }}</span>
        <button class="primary-button primary-button--small" type="button" @click="submitMoveCurrentNote">确认移动</button>
      </div>
    </div>

    <div class="directory-section directory-section--full">
      <div class="note-cards note-cards--compact note-cards--grouped">
        <section v-for="group in groupedNotes" :key="group.folder" class="directory-group">
          <div class="directory-group__header">
            <span>{{ group.folder }}</span>
            <small>{{ group.notes.length }} 篇</small>
          </div>
          <article v-for="note in group.notes" :key="note.id" class="note-card note-card--slim" @click="openNote(note.slug)">
            <h3>{{ note.title }}</h3>
            <p>{{ note.summary }}</p>
          </article>
        </section>
      </div>
    </div>

    <div class="tag-filter-bar">
      <button class="ghost-button ghost-button--small" type="button" @click="store.clearTagFilter()">全部</button>
      <button
        v-for="tag in visibleTags"
        :key="tag.tag"
        class="tag-chip tag-chip--button"
        :class="{ 'tag-chip--active': activeTagFilter === tag.tag }"
        type="button"
        @click="store.setTagFilter(tag.tag)"
      >
        #{{ tag.tag }} {{ tag.count }}
      </button>
    </div>

    <div class="tag-cloud tag-cloud--footer">
      <span class="helper-text">{{ activeTagFilter ? `当前筛选 #${activeTagFilter}` : '显示全部笔记' }}</span>
    </div>
  </section>
</template>
