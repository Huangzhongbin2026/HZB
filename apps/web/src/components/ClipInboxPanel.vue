<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { clipPreview, clipSummaryPrompt, clipSummarizeWithAi, clipUrl, errorMessage, isBusy, showClipPreview } = storeToRefs(store)
</script>

<template>
  <section class="surface-card home-panel">
    <header class="surface-header">
      <div>
        <p class="panel-eyebrow">Clipper</p>
        <h2>添加连接</h2>
      </div>
      <button class="primary-button panel-top-action clip-action-button" type="button" :disabled="isBusy" @click="store.submitClip()">
        {{ isBusy ? '解析中...' : '解析' }}
      </button>
    </header>

    <input
      v-model="clipUrl"
      class="field-input"
      placeholder="粘贴文章链接，解析正文后可预览并保存为笔记"
    />

    <label class="toggle-chip clip-toggle">
      <input v-model="clipSummarizeWithAi" type="checkbox" />
      <span>AI 提炼总结</span>
    </label>

    <textarea
      v-if="clipSummarizeWithAi"
      v-model="clipSummaryPrompt"
      class="field-input clip-textarea clip-textarea--compact"
      placeholder="例如：提炼 5 个要点，并给出适合沉淀到知识库的结构化总结"
    />

    <p v-if="errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>

    <div v-if="showClipPreview && clipPreview" class="modal-backdrop" @click.self="store.closeClipPreview()">
      <div class="modal-card modal-card--wide">
        <div class="modal-card__header">
          <div>
            <h3>{{ clipSummarizeWithAi ? '提炼预览' : '解析预览' }}</h3>
            <p class="helper-text">{{ clipPreview.extracted_title }}</p>
          </div>
          <button class="ghost-button ghost-button--small" type="button" @click="store.closeClipPreview()">关闭</button>
        </div>
        <div class="modal-card__body">
          <pre class="ai-output ai-output--large clip-preview-output">{{ clipPreview.preview_content }}</pre>
        </div>
        <div class="modal-card__footer">
          <button class="ghost-button modal-card__action" type="button" @click="store.closeClipPreview()">取消</button>
          <button class="primary-button modal-card__action modal-card__action--primary" type="button" :disabled="isBusy" @click="store.saveClipPreview()">同意并保存</button>
        </div>
      </div>
    </div>
  </section>
</template>
