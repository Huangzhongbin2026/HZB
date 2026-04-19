<template>
  <MobileShell current="templates" title="模板" subtitle="像 Obsidian Templates 一样集中管理常用结构。" :show-settings="true">
    <view class="section-card">
      <text class="section-title">模板库</text>
      <view class="template-list">
        <view v-for="item in templateNotes" :key="item.slug" class="template-item">
          <text class="template-item__title">{{ item.title }}</text>
          <text class="template-item__path">{{ item.path }}</text>
          <button class="secondary-button" @click="openTemplate(item.slug)">查看模板</button>
        </view>
      </view>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import MobileShell from '../../components/MobileShell.vue'
import { bootstrapMobileWorkspace, mobileWorkspace, selectNote } from '../../utils/mobileWorkspace'

const state = mobileWorkspace
const templateNotes = computed(() => state.notes.filter((note) => note.path.startsWith('04_Templates/')))

onShow(async () => {
  await bootstrapMobileWorkspace()
})

async function openTemplate(slug: string) {
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

.section-title,
.template-item__title {
  display: block;
  color: var(--mobile-text);
  font-size: 32rpx;
  font-weight: 700;
}

.template-list {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
}

.template-item {
  display: grid;
  gap: 10rpx;
  padding: 20rpx;
  border-radius: var(--mobile-radius-control);
  background: rgba(255, 255, 255, 0.72);
}

.template-item__path {
  color: var(--mobile-muted);
  font-size: 24rpx;
}

.secondary-button {
  min-height: 76rpx;
  padding: 0 24rpx;
  border-radius: var(--mobile-radius-control);
  border: 1px solid var(--mobile-border);
  background: rgba(255, 252, 247, 0.86);
  color: var(--mobile-text);
}

.secondary-button::after {
  border: 0;
}
</style>