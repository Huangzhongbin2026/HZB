<template>
  <MobileShell current="notes" title="笔记" subtitle="用图标、树目录和信息栏把手机端也收进同一套工作流。" :show-settings="true">
    <view class="section-card form-card">
      <view class="section-head section-head--actions">
        <view>
          <text class="section-title">搜索与定位</text>
          <text class="section-desc">目录、文件夹、标签和正文都可以快速定位。</text>
        </view>
        <view class="icon-row">
          <button class="icon-button" @click="createNote">＋</button>
          <button class="icon-button" @click="promptFolder">📁</button>
        </view>
      </view>
      <input v-model="state.searchQuery" class="field-input" placeholder="搜索笔记、标签或正文片段" @confirm="search" />
      <button class="secondary-button" @click="search">筛选结果</button>
      <scroll-view v-if="state.searchResults.length > 0" scroll-x class="chip-scroll">
        <view class="chip-row">
          <text v-for="item in state.searchResults.slice(0, 8)" :key="item.note_slug" class="chip" @click="openNote(item.note_slug)">
            {{ item.title }}
          </text>
        </view>
      </scroll-view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">文件树</text>
        <text class="section-desc">支持多层文件夹嵌套，移动端用树形层级取代平铺列表。</text>
      </view>
      <view class="tree-stack">
        <MobileTreeNode v-for="node in state.fileTree" :key="node.path" :node="node" :active-slug="state.currentNote?.slug" @select="openNote" />
      </view>
    </view>

    <view v-if="state.currentNote" class="section-card detail-card">
      <text class="detail-title">{{ state.currentNote.title }}</text>
      <text class="detail-path">{{ state.currentNote.path }}</text>
      <text class="detail-content">{{ state.currentNote.content }}</text>

      <view class="meta-panel-grid">
        <view class="meta-group">
          <text class="meta-title">标签</text>
          <view class="chip-row">
            <text v-for="tag in state.currentNote.tags" :key="tag" class="chip">#{{ tag }}</text>
            <text v-if="state.currentNote.tags.length === 0" class="empty-hint">暂无标签</text>
          </view>
        </view>

        <view class="meta-group">
          <text class="meta-title">出链</text>
          <view class="chip-row">
            <text v-for="slug in state.outgoingLinks" :key="slug" class="chip chip--soft">[[{{ slug }}]]</text>
            <text v-if="state.outgoingLinks.length === 0" class="empty-hint">暂无出链</text>
          </view>
        </view>

        <view class="meta-group">
          <text class="meta-title">反链</text>
          <view class="chip-row">
            <text v-for="slug in state.incomingLinks" :key="slug" class="chip chip--warm">{{ slug }}</text>
            <text v-if="state.incomingLinks.length === 0" class="empty-hint">暂无反链</text>
          </view>
        </view>

        <view class="meta-group">
          <text class="meta-title">属性</text>
          <text class="empty-hint">更新时间：{{ state.currentNote.updated_at || '未知' }}</text>
          <text class="empty-hint">标签数：{{ state.currentNote.tags.length }}</text>
          <text class="empty-hint">路径：{{ state.currentNote.path }}</text>
        </view>
      </view>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'

import MobileShell from '../../components/MobileShell.vue'
import MobileTreeNode from '../../components/MobileTreeNode.vue'
import { bootstrapMobileWorkspace, createFolder, createQuickNote, mobileWorkspace, searchNotes, selectNote } from '../../utils/mobileWorkspace'

const state = mobileWorkspace

onShow(async () => {
  await bootstrapMobileWorkspace()
})

async function search() {
  await searchNotes()
}

async function openNote(slug: string) {
  await selectNote(slug)
}

async function createNote() {
  await createQuickNote()
}

function promptFolder() {
  uni.showModal({
    title: '新建文件夹',
    editable: true,
    placeholderText: '例如 01_Notes/项目/会议',
    success: async (result) => {
      if (!result.confirm || !result.content) {
        return
      }
      await createFolder(result.content)
    },
  })
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

.form-card {
  display: grid;
  gap: 16rpx;
}

.section-head {
  margin-bottom: 16rpx;
}

.section-head--actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.section-title,
.detail-title,
.note-list-item__title,
.meta-title {
  display: block;
  color: var(--mobile-text);
  font-size: 32rpx;
  font-weight: 700;
}

.section-desc,
.detail-path,
.note-list-item__path,
.note-list-item__summary,
.empty-hint {
  color: var(--mobile-muted);
  font-size: 24rpx;
  line-height: 1.6;
}

.field-input {
  width: auto;
  min-height: 86rpx;
  padding: 0 24rpx;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-control);
  background: var(--mobile-surface-strong);
  color: var(--mobile-text);
  font-size: 28rpx;
}

.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  margin: 0;
  padding: 0 28rpx;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-control);
  background: rgba(255, 252, 247, 0.86);
  color: var(--mobile-text);
  font-size: 28rpx;
}

.icon-row {
  display: flex;
  gap: 12rpx;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 76rpx;
  height: 76rpx;
  margin: 0;
  padding: 0;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-control);
  background: var(--mobile-surface);
  color: var(--mobile-text);
  font-size: 32rpx;
}

.icon-button::after {
  border: 0;
}

.secondary-button::after {
  border: 0;
}

.chip-scroll {
  white-space: nowrap;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(83, 71, 60, 0.08);
  color: var(--mobile-text);
  font-size: 24rpx;
}

.chip--soft {
  background: rgba(95, 129, 165, 0.12);
}

.chip--warm {
  background: rgba(181, 142, 85, 0.14);
}

.note-list-item {
  display: grid;
  gap: 8rpx;
  padding: 20rpx 0;
  border-bottom: 1px solid rgba(121, 104, 85, 0.08);
}

.tree-stack,
.meta-panel-grid {
  display: grid;
  gap: 14rpx;
}

.note-list-item--active {
  opacity: 0.9;
}

.note-list-item:last-child {
  border-bottom: 0;
}

.detail-card {
  display: grid;
  gap: 16rpx;
}

.detail-content {
  color: var(--mobile-text);
  font-size: 26rpx;
  line-height: 1.8;
  white-space: pre-wrap;
}

.meta-group {
  display: grid;
  gap: 10rpx;
}
</style>