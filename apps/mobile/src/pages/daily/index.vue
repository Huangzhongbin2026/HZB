<template>
  <MobileShell current="daily" title="日记" subtitle="像 Obsidian Daily Notes 一样，快速进入今日日志。" :show-settings="true">
    <view class="section-card">
      <view class="section-head">
        <text class="section-title">今日日记</text>
        <button class="primary-button" @click="createToday">创建 / 打开</button>
      </view>
      <text class="section-desc">默认保存到 05_Daily 目录，延续桌面端和网页端的统一结构。</text>
    </view>

    <view class="section-card">
      <text class="section-title">历史日记</text>
      <view class="daily-list">
        <view v-for="item in dailyNotes" :key="item.slug" class="daily-item" @click="openNote(item.slug)">
          <text class="daily-item__title">{{ item.title }}</text>
          <text class="daily-item__path">{{ item.path }}</text>
        </view>
      </view>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import MobileShell from '../../components/MobileShell.vue'
import { bootstrapMobileWorkspace, createQuickNote, mobileWorkspace, selectNote } from '../../utils/mobileWorkspace'

const state = mobileWorkspace
const dailyNotes = computed(() => state.notes.filter((note) => note.path.startsWith('05_Daily/') || note.path.startsWith('06_Diary/')))

onShow(async () => {
  await bootstrapMobileWorkspace()
})

async function createToday() {
  const today = new Date().toISOString().slice(0, 10)
  const matched = dailyNotes.value.find((note) => note.title === today)
  if (matched) {
    await openNote(matched.slug)
    return
  }
  await createQuickNote('05_Daily')
}

async function openNote(slug: string) {
  await selectNote(slug)
  uni.reLaunch({ url: '/src/pages/notes/index' })
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

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.section-title,
.daily-item__title {
  display: block;
  color: var(--mobile-text);
  font-size: 32rpx;
  font-weight: 700;
}

.section-desc,
.daily-item__path {
  color: var(--mobile-muted);
  font-size: 24rpx;
}

.primary-button {
  min-height: 76rpx;
  padding: 0 24rpx;
  border-radius: var(--mobile-radius-control);
  border: 0;
  background: linear-gradient(180deg, #2f2924, #1d1814);
  color: #fff8ef;
}

.primary-button::after {
  border: 0;
}

.daily-list {
  display: grid;
  gap: 14rpx;
  margin-top: 16rpx;
}

.daily-item {
  padding: 20rpx;
  border-radius: var(--mobile-radius-control);
  background: rgba(255, 255, 255, 0.72);
}
</style>