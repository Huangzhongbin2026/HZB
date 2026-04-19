<template>
  <MobileShell current="ai" title="创作" subtitle="总结、扩写、润色、问答和保存为新笔记都收敛到一个触屏工作流。" :show-settings="true">
    <view class="section-card form-card">
      <text class="section-title">创作上下文</text>
      <scroll-view scroll-x class="note-scroll">
        <view class="note-chip-row">
          <text
            v-for="item in state.notes.slice(0, 12)"
            :key="item.slug"
            class="note-chip"
            :class="{ 'note-chip--active': state.currentNote?.slug === item.slug }"
            @click="focusNote(item.slug)"
          >
            {{ item.title }}
          </text>
        </view>
      </scroll-view>
      <view class="toggle-row">
        <text class="toggle-label">连接远端模型</text>
        <switch :checked="state.useRemoteAi" @change="state.useRemoteAi = $event.detail.value" />
      </view>
      <view class="toggle-row">
        <text class="toggle-label">带入 RAG 参考</text>
        <switch :checked="state.useRag" @change="state.useRag = $event.detail.value" />
      </view>
      <textarea v-model="state.aiPrompt" class="field-textarea" placeholder="输入要总结、扩写、润色、问答或创作的内容" />
    </view>

    <view class="section-card">
      <text class="section-title">创作动作</text>
      <view class="action-grid">
        <button class="secondary-button" :loading="state.isLoading" @click="run('summary')">总结</button>
        <button class="secondary-button" :loading="state.isLoading" @click="run('expand')">扩写</button>
        <button class="secondary-button" :loading="state.isLoading" @click="run('polish')">润色</button>
        <button class="secondary-button" :loading="state.isLoading" @click="run('qa')">问答</button>
      </view>
      <button class="primary-button" :loading="state.isLoading" @click="run('create', true)">创作并保存为笔记</button>
      <button class="secondary-button" @click="uploadDocument">上传文档并提炼</button>
      <text v-if="state.uploadedDocumentSummary" class="status-hint">{{ state.uploadedDocumentSummary }}</text>
    </view>

    <view class="section-card">
      <text class="section-title">生成结果</text>
      <text class="result-body">{{ state.aiResult || '生成结果会显示在这里，适合直接阅读或继续追加到知识库。' }}</text>
      <view v-if="state.aiReferences.length > 0" class="reference-stack">
        <view v-for="item in state.aiReferences" :key="`${item.note_slug}-${item.title}`" class="reference-card">
          <text class="reference-title">{{ item.title }}</text>
          <text class="reference-meta">相关度 {{ item.score }}</text>
          <text class="reference-snippet">{{ item.snippet }}</text>
        </view>
      </view>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'

import MobileShell from '../../components/MobileShell.vue'
import { bootstrapMobileWorkspace, mobileWorkspace, runAi, selectNote, uploadDocument as uploadDoc } from '../../utils/mobileWorkspace'

const state = mobileWorkspace

onShow(async () => {
  await bootstrapMobileWorkspace()
})

async function focusNote(slug: string) {
  await selectNote(slug)
}

async function run(action: 'summary' | 'expand' | 'polish' | 'qa' | 'create', saveAsNote = false) {
  await runAi(action, saveAsNote)
}

function uploadDocument() {
  uploadDoc()
}
</script>

<style scoped>
.section-card {
  margin-bottom: 22rpx;
  padding: var(--mobile-space-card);
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-card);
  background: var(--mobile-surface);
  box-shadow: var(--mobile-shadow);
}

.form-card,
.reference-stack {
  display: grid;
  gap: 16rpx;
}

.section-title,
.reference-title {
  display: block;
  color: var(--mobile-text);
  font-size: 32rpx;
  font-weight: 700;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.toggle-label,
.reference-meta,
.status-hint {
  color: var(--mobile-muted);
  font-size: 24rpx;
}

.field-textarea {
  width: auto;
  min-height: 220rpx;
  padding: 20rpx 24rpx;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-control);
  background: var(--mobile-surface-strong);
  color: var(--mobile-text);
  font-size: 28rpx;
}

.note-scroll {
  white-space: nowrap;
}

.note-chip-row,
.action-grid {
  display: grid;
  gap: 14rpx;
}

.note-chip-row {
  display: flex;
}

.note-chip {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 18rpx;
  margin-right: 12rpx;
  border-radius: 999rpx;
  background: rgba(83, 71, 60, 0.08);
  color: var(--mobile-text);
  font-size: 24rpx;
}

.note-chip--active {
  background: #2f2924;
  color: #fff8ef;
}

.action-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 18rpx 0;
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  margin: 0;
  padding: 0 28rpx;
  border-radius: var(--mobile-radius-control);
  font-size: 28rpx;
}

.primary-button {
  border: 0;
  background: linear-gradient(180deg, #2f2924, #1d1814);
  color: #fff8ef;
}

.secondary-button {
  border: 1px solid var(--mobile-border);
  background: rgba(255, 252, 247, 0.86);
  color: var(--mobile-text);
}

.primary-button::after,
.secondary-button::after {
  border: 0;
}

.result-body,
.reference-snippet {
  color: var(--mobile-text);
  font-size: 26rpx;
  line-height: 1.8;
  white-space: pre-wrap;
}

.reference-card {
  padding: 20rpx;
  border-radius: var(--mobile-radius-control);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--mobile-border);
}

.reference-meta {
  display: block;
  margin: 6rpx 0 10rpx;
}
</style>