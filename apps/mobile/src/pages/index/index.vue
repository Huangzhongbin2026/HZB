<template>
  <MobileShell current="home" title="个人知识库" subtitle="把搜索、剪藏、同步和最近内容都放到手机首页。" :show-settings="true">
    <view class="overview-grid">
      <view class="overview-card overview-card--dark">
        <text class="overview-label">同步状态</text>
        <text class="overview-value">{{ syncStatusText }}</text>
        <text class="overview-meta">{{ state.syncStatus.pending_events }} 个待同步事件</text>
      </view>
      <view class="overview-card">
        <text class="overview-label">知识库规模</text>
        <text class="overview-value">{{ state.notes.length }} 篇</text>
        <text class="overview-meta">{{ state.tags.length }} 个标签</text>
      </view>
      <view class="overview-card">
        <text class="overview-label">部署模式</text>
        <text class="overview-value">{{ state.instanceConfig?.deployment_mode === 'server' ? '服务器' : '主电脑' }}</text>
        <text class="overview-meta">{{ state.instanceConfig?.edition === 'team' ? '团队版实例' : '个人版实例' }}</text>
      </view>
      <view class="overview-card">
        <text class="overview-label">授权状态</text>
        <text class="overview-value">{{ state.instanceConfig?.team_license_verified ? '已授权' : '未授权' }}</text>
        <text class="overview-meta">{{ state.instanceConfig?.instance_name || '未命名实例' }}</text>
      </view>
    </view>

    <view class="section-card form-card">
      <view class="section-head">
        <text class="section-title">云端连接</text>
        <text class="section-desc">首次打开会自动尝试连接，本地开发可直接使用默认账号。</text>
      </view>
      <input v-model="state.username" class="field-input" placeholder="账号" />
      <input v-model="state.password" class="field-input" password placeholder="密码" />
      <button class="primary-button" :loading="state.isLoading" @click="login">连接知识库</button>
    </view>

    <view class="section-card form-card">
      <view class="section-head">
        <text class="section-title">全局搜索</text>
        <text class="section-desc">快速找笔记、标签和片段，移动端优先返回可直接阅读的结果。</text>
      </view>
      <input v-model="state.searchQuery" class="field-input" placeholder="搜索标题、正文或标签" @confirm="search" />
      <button class="secondary-button" @click="search">开始搜索</button>
      <view v-if="state.searchResults.length > 0" class="result-stack">
        <view v-for="item in state.searchResults.slice(0, 4)" :key="item.note_slug" class="result-card" @click="openSearchResult(item.note_slug)">
          <text class="result-title">{{ item.title }}</text>
          <text class="result-path">{{ item.path }}</text>
          <text class="result-snippet">{{ item.snippet || '点击查看完整内容' }}</text>
        </view>
      </view>
    </view>

    <view class="section-card form-card">
      <view class="section-head">
        <text class="section-title">添加连接</text>
        <text class="section-desc">和网页端一致，先预览抓取结果，再决定是否保存进 Inbox。</text>
      </view>
      <input v-model="state.clipUrl" class="field-input" placeholder="粘贴网页或公众号文章链接" />
      <view class="toggle-row">
        <text class="toggle-label">抓取后自动提炼</text>
        <switch :checked="state.clipSummarizeWithAi" @change="state.clipSummarizeWithAi = $event.detail.value" />
      </view>
      <textarea
        v-if="state.clipSummarizeWithAi"
        v-model="state.clipSummaryPrompt"
        class="field-textarea field-textarea--compact"
        placeholder="例如：提炼 5 个要点，保留适合沉淀到知识库的摘要"
      />
      <button class="primary-button" :loading="state.isLoading" @click="previewClip">预览后保存</button>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">最近更新</text>
        <text class="section-desc">优先展示最近可继续阅读或继续创作的内容。</text>
      </view>
      <view v-for="item in recentNotes" :key="item.slug" class="note-row" @click="openNote(item.slug)">
        <view>
          <text class="note-row__title">{{ item.title }}</text>
          <text class="note-row__summary">{{ item.summary || item.path }}</text>
        </view>
        <text class="note-row__arrow">›</text>
      </view>
    </view>

    <view v-if="state.clipPreviewOpen && state.clipPreview" class="preview-mask" @click="closePreview">
      <view class="preview-sheet" @click.stop>
        <text class="preview-title">{{ state.clipPreview.extracted_title }}</text>
        <text class="preview-url">{{ state.clipPreview.source_url }}</text>
        <scroll-view scroll-y class="preview-scroll">
          <text class="preview-summary">{{ state.clipPreview.summary || '未生成摘要' }}</text>
          <text class="preview-content">{{ state.clipPreview.preview_content }}</text>
        </scroll-view>
        <view class="preview-actions">
          <button class="secondary-button" @click="closePreview">取消</button>
          <button class="primary-button" :loading="state.isLoading" @click="savePreview">保存到 Inbox</button>
        </view>
      </view>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import MobileShell from '../../components/MobileShell.vue'
import {
  bootstrapMobileWorkspace,
  closeClipPreview,
  login as connectWorkspace,
  mobileWorkspace,
  saveClipPreview,
  searchNotes,
  selectNote,
  submitClipPreview,
} from '../../utils/mobileWorkspace'

const state = mobileWorkspace

const recentNotes = computed(() => state.notes.slice(0, 5))
const syncStatusText = computed(() => {
  if (!state.isAuthenticated) {
    return '未连接'
  }
  if (state.syncStatus.pending_events > 0) {
    return '同步中'
  }
  return '已同步'
})

onShow(async () => {
  await bootstrapMobileWorkspace()
})

async function login() {
  await connectWorkspace()
}

async function search() {
  await searchNotes()
}

async function openSearchResult(slug: string) {
  await selectNote(slug)
  uni.reLaunch({ url: '/src/pages/notes/index' })
}

async function openNote(slug: string) {
  await selectNote(slug)
  uni.reLaunch({ url: '/src/pages/notes/index' })
}

async function previewClip() {
  await submitClipPreview()
}

function closePreview() {
  closeClipPreview()
}

async function savePreview() {
  await saveClipPreview()
}
</script>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
  margin-bottom: 22rpx;
}

.overview-card,
.section-card,
.result-card {
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-card);
  background: var(--mobile-surface);
  box-shadow: var(--mobile-shadow);
}

.overview-card {
  display: grid;
  gap: 10rpx;
  padding: 24rpx;
}

.overview-card--dark {
  background: linear-gradient(180deg, #3c342d, #211c18);
}

.overview-card--dark .overview-label,
.overview-card--dark .overview-meta,
.overview-card--dark .overview-value {
  color: #fff3e4;
}

.overview-label,
.overview-meta,
.section-desc,
.result-path,
.result-snippet,
.preview-url {
  color: var(--mobile-muted);
}

.overview-label,
.section-desc,
.result-path {
  font-size: 24rpx;
}

.overview-value,
.section-title,
.preview-title {
  display: block;
  color: var(--mobile-text);
  font-size: 34rpx;
  font-weight: 700;
}

.overview-value {
  font-size: 40rpx;
}

.overview-meta,
.note-row__summary,
.preview-summary,
.preview-content {
  font-size: 24rpx;
  line-height: 1.7;
}

.section-card {
  margin-bottom: 22rpx;
  padding: 26rpx;
}

.section-head {
  margin-bottom: 18rpx;
}

.section-desc {
  display: block;
  margin-top: 8rpx;
  line-height: 1.6;
}

.form-card {
  display: grid;
  gap: 16rpx;
}

.field-input,
.field-textarea {
  width: auto;
  min-height: 86rpx;
  padding: 0 24rpx;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-control);
  background: var(--mobile-surface-strong);
  color: var(--mobile-text);
  font-size: 28rpx;
}

.field-textarea {
  min-height: 178rpx;
  padding: 20rpx 24rpx;
}

.field-textarea--compact {
  min-height: 120rpx;
}

.toggle-row,
.preview-actions,
.note-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.toggle-label,
.note-row__summary,
.result-snippet {
  color: var(--mobile-muted);
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  margin: 0;
  padding: 0 28rpx;
  border-radius: 22rpx;
  font-size: 28rpx;
  line-height: 1;
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

.result-stack {
  display: grid;
  gap: 14rpx;
}

.result-card {
  display: grid;
  gap: 8rpx;
  padding: 20rpx;
}

.result-title,
.note-row__title {
  display: block;
  color: var(--mobile-text);
  font-size: 30rpx;
  font-weight: 600;
}

.note-row {
  padding: 18rpx 0;
  border-bottom: 1px solid rgba(121, 104, 85, 0.08);
}

.note-row:last-child {
  border-bottom: 0;
}

.note-row__arrow {
  color: #998a7c;
  font-size: 34rpx;
}

.preview-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 24rpx;
  background: rgba(28, 23, 19, 0.34);
}

.preview-sheet {
  width: 100%;
  max-height: 78vh;
  padding: 28rpx;
  border-radius: 34rpx;
  background: #fffcf7;
}

.preview-scroll {
  max-height: 46vh;
  margin: 20rpx 0 24rpx;
}

.preview-summary,
.preview-content {
  display: block;
  color: #4a4037;
  white-space: pre-wrap;
}

.preview-content {
  margin-top: 16rpx;
}
</style>
  margin-bottom: 24rpx;
}

.tab-button,
.action-button,
.chip-button {
  padding: 18rpx 24rpx;
  border-radius: 999rpx;
  background: #f4f4f5;
  color: #303133;
}

.tab-button--active,
.action-button {
  background: #409eff;
  color: #ffffff;
}

.action-button--ghost {
  background: #ecf5ff;
  color: #409eff;
}

.field-input,
.field-textarea {
  width: 100%;
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 12rpx;
  background: #fff;
  color: #303133;
}

.field-textarea {
  min-height: 220rpx;
}

.field-textarea--compact {
  min-height: 140rpx;
}

.title,
.section-title,
.note-title,
.graph-node-title,
.meta-title {
  display: block;
  color: #303133;
}

.title {
  margin: 8rpx 0 10rpx;
  font-size: 36rpx;
  font-weight: 700;
}

.desc,
.ai-output,
.content-preview {
  color: #606266;
  line-height: 1.7;
}

.note-desc {
  color: #909399;
}

.status-pill,
.meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #ecf5ff;
  color: #409eff;
  font-size: 24rpx;
}

.meta-pill--soft {
  background: #f4f4f5;
  color: #606266;
}

.section-stack {
  display: grid;
  gap: 24rpx;
}

.note-item,
.graph-node-card {
  display: grid;
  gap: 10rpx;
  padding: 18rpx 0;
  border-bottom: 1px solid #ebeef5;
}

.chip-scroll {
  width: 100%;
  white-space: nowrap;
  margin-top: 18rpx;
}

.chip-row {
  display: inline-flex;
}

.graph-grid {
  display: grid;
  gap: 18rpx;
  margin-top: 18rpx;
}

.meta-group,
.form-stack {
  display: grid;
  gap: 12rpx;
  margin-top: 18rpx;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
}
</style>
