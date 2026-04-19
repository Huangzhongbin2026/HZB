<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

import { useWorkspaceStore } from '../stores/workspace'

const editorRef = ref<HTMLDivElement | null>(null)
const editor = ref<Vditor | null>(null)
const store = useWorkspaceStore()
const { currentNote, syncStatus } = storeToRefs(store)

onMounted(() => {
  if (!editorRef.value) {
    return
  }

  editor.value = new Vditor(editorRef.value, {
    minHeight: 520,
    cache: { enable: false },
    toolbarConfig: { pin: true },
    value: currentNote.value?.content ?? '# 欢迎\n\n请先登录并加载笔记。',
    placeholder: '在这里输入 [[双向链接]]、#标签 与想法片段',
    input(value) {
      store.updateCurrentContent(value)
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
</script>

<template>
  <section class="editor-panel">
    <header class="panel-header panel-header--bordered">
      <div>
        <p class="panel-eyebrow">Editor</p>
        <input
          :value="currentNote?.title ?? ''"
          class="title-input"
          placeholder="笔记标题"
          @input="store.updateCurrentTitle(($event.target as HTMLInputElement).value)"
        />
        <span class="note-path">{{ currentNote?.path ?? '请选择一篇笔记' }}</span>
      </div>
      <div class="panel-actions">
        <button class="ghost-button" type="button" @click="store.pollSyncStatus()">
          {{ syncStatus.pending_events }} 待同步
        </button>
        <button class="ghost-button" type="button" @click="store.deleteCurrentNote()">删除当前笔记</button>
        <button class="primary-button" type="button" @click="store.saveCurrentNote()">同步到云端</button>
      </div>
    </header>
    <div ref="editorRef" class="editor-host"></div>
  </section>
</template>
