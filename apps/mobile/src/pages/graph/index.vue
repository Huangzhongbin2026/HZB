<template>
  <MobileShell current="graph" title="图谱" subtitle="移动端不强行塞复杂画布，而是优先给关系热点、标签聚合和可跳转节点。" :show-settings="true">
    <view class="section-card">
      <text class="section-title">关系概览</text>
      <text class="section-desc">{{ statusText }}</text>
    </view>

    <view class="section-card">
      <text class="section-title">核心节点</text>
      <view class="graph-grid">
        <view v-for="item in graphNodes" :key="item.slug" class="graph-card" @click="openNote(item.slug)">
          <text class="graph-title">{{ item.title }}</text>
          <text class="section-desc">{{ item.path }}</text>
          <view class="pill-row">
            <text class="pill">{{ item.links.length }} 出链</text>
            <text class="pill pill--soft">{{ item.backlinkCount }} 反链</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section-card" v-if="state.tags.length > 0">
      <text class="section-title">标签热区</text>
      <view class="pill-row">
        <text v-for="tag in state.tags.slice(0, 12)" :key="tag.tag" class="pill">#{{ tag.tag }} {{ tag.count }}</text>
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

const graphNodes = computed(() =>
  state.notes.slice(0, 10).map((note) => ({
    ...note,
    backlinkCount: state.notes.filter((candidate) => candidate.links.includes(note.slug)).length,
  })),
)

const statusText = computed(() => {
  if (!state.isAuthenticated) {
    return '尚未连接云端，当前图谱只显示静态提示。'
  }
  return `已加载 ${state.notes.length} 篇笔记的关系摘要。点击节点可直接打开笔记详情。`
})

onShow(async () => {
  await bootstrapMobileWorkspace()
})

async function openNote(slug: string) {
  await selectNote(slug)
  uni.reLaunch({ url: '/src/pages/notes/index' })
}
</script>

<style scoped>
.section-card {
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: var(--mobile-radius-card);
  background: var(--mobile-surface);
  border: 1px solid var(--mobile-border);
  box-shadow: var(--mobile-shadow);
}

.section-desc {
  color: var(--mobile-muted);
}

.section-title,
.graph-title {
  display: block;
  color: var(--mobile-text);
  font-size: 34rpx;
  font-weight: 700;
}

.section-desc {
  margin-top: 12rpx;
  line-height: 1.7;
  font-size: 24rpx;
}

.graph-grid {
  display: grid;
  gap: 18rpx;
  margin-top: 18rpx;
}

.graph-card {
  display: grid;
  gap: 10rpx;
  padding: 18rpx 0;
  border-bottom: 1px solid rgba(86, 74, 62, 0.08);
}

.graph-card:last-child {
  border-bottom: 0;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 10rpx;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(86, 74, 62, 0.08);
  color: var(--mobile-text);
  font-size: 24rpx;
}

.pill--soft {
  background: rgba(104, 132, 179, 0.12);
}
</style>
