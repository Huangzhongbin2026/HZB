<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()
const uploadError = ref('')
const {
  aiPrompt,
  aiReferences,
  aiResponse,
  aiUseRag,
  aiUseRemote,
  currentNote,
  errorMessage,
  isBusy,
  showAiPreview,
  sortedNotes,
  uploadedDocument,
} = storeToRefs(store)

function openReference(slug?: string) {
  if (!slug) {
    return
  }
  router.push(`/notes/${slug}`)
}

async function onDocumentChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) {
    return
  }
  uploadError.value = ''
  try {
    await store.uploadDocument(file)
  } catch {
    uploadError.value = '文档上传失败，请确认文件类型与服务端状态。'
  }
}
</script>

<template>
  <section class="surface-card ai-studio-panel">
    <header class="surface-header">
      <div>
        <h2>创作中心</h2>
      </div>
      <span class="status-pill">上下文 {{ aiReferences.length }}</span>
    </header>

    <div class="ai-studio-grid">
      <div class="sub-panel ai-form-panel">
        <span class="section-title">当前焦点笔记</span>
        <select class="field-input" :value="currentNote?.slug ?? ''" @change="store.selectNote(($event.target as HTMLSelectElement).value)">
          <option v-for="note in sortedNotes" :key="note.slug" :value="note.slug">{{ note.title }}</option>
        </select>
        <div class="ai-switch-row">
          <label class="toggle-chip">
            <input v-model="aiUseRemote" type="checkbox" />
            <span>创作时连接 AI</span>
          </label>
          <label class="toggle-chip">
            <input v-model="aiUseRag" type="checkbox" />
            <span>带入 RAG 参考</span>
          </label>
          <button class="ghost-button" type="button" @click="store.openSettings('models')">打开设置</button>
        </div>
        <textarea v-model="aiPrompt" class="ai-textarea ai-textarea--large" placeholder="输入创作要求，例如生成文章框架、重写摘要、基于知识库起草内容" />
        <div class="ai-button-row">
          <button class="ghost-button" type="button" :disabled="isBusy" @click="store.runAi('summary')">总结</button>
          <button class="ghost-button" type="button" :disabled="isBusy" @click="store.runAi('expand')">扩写</button>
          <button class="ghost-button" type="button" :disabled="isBusy" @click="store.runAi('polish')">润色</button>
          <button class="ghost-button" type="button" :disabled="isBusy" @click="store.runAi('qa')">问答</button>
          <button class="primary-button" type="button" :disabled="isBusy" @click="store.runAi('create', true)">{{ isBusy ? '处理中...' : '创作并保存' }}</button>
        </div>
        <p v-if="errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
      </div>

      <div class="sub-panel ai-result-panel">
        <span class="section-title">生成结果</span>
        <div class="ai-output ai-output--large">{{ aiResponse || '结果会显示在这里。' }}</div>
        <div class="ai-result-actions">
          <button
            class="primary-button ai-result-actions__button"
            :class="{ 'ai-result-actions__button--hidden': !showAiPreview || !aiResponse }"
            type="button"
            :disabled="!showAiPreview || !aiResponse"
            @click="store.applyAiResultToCurrentNote()"
          >
            追加到当前笔记
          </button>
        </div>
      </div>
    </div>

    <div class="sub-panel ai-upload-panel">
      <div>
        <span class="section-title">文档提炼</span>
        <p class="helper-text">支持 txt、md、docx、pptx、pdf，上传后可提炼总结并直接存入知识库。</p>
      </div>
      <input class="field-input" type="file" accept=".txt,.md,.docx,.pptx,.pdf" @change="onDocumentChange" />
      <p v-if="uploadError" class="helper-text helper-text--error">{{ uploadError }}</p>
      <div v-if="uploadedDocument" class="document-preview">
        <strong>{{ uploadedDocument.extracted_title }}</strong>
        <p>{{ uploadedDocument.summary || uploadedDocument.content_preview }}</p>
      </div>
    </div>

    <div class="reference-list reference-list--block">
      <button
        v-for="item in aiReferences"
        :key="`${item.note_slug}-${item.score}`"
        class="reference-card"
        type="button"
        @click="openReference(item.note_slug)"
      >
        <strong>{{ item.title }}</strong>
        <span>{{ item.score }}</span>
        <p>{{ item.snippet }}</p>
      </button>
    </div>
  </section>
</template>
