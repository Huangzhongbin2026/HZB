<template>
  <MobileShell current="whiteboard" title="白板" subtitle="移动端提供轻量白板入口，用于快速记录卡片式想法。" :show-settings="true">
    <view class="section-card">
      <view class="section-head">
        <text class="section-title">白板草稿</text>
        <button class="primary-button" @click="addCard">添加卡片</button>
      </view>
      <view class="board-grid">
        <view v-for="item in cards" :key="item.id" class="board-card">
          <input v-model="item.title" class="board-input board-input--title" placeholder="卡片标题" />
          <textarea v-model="item.body" class="board-input board-input--body" placeholder="记录想法、关系或待办" />
        </view>
      </view>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

import MobileShell from '../../components/MobileShell.vue'

type Card = {
  id: string
  title: string
  body: string
}

const storageKey = 'knowledge-cloud-mobile-board'
const cards = ref<Card[]>(JSON.parse(uni.getStorageSync(storageKey) || '[]'))

watch(cards, (value) => {
  uni.setStorageSync(storageKey, JSON.stringify(value))
}, { deep: true })

function addCard() {
  cards.value = [...cards.value, { id: String(Date.now()), title: '新卡片', body: '' }]
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

.section-title {
  color: var(--mobile-text);
  font-size: 32rpx;
  font-weight: 700;
}

.board-grid {
  display: grid;
  gap: 16rpx;
  margin-top: 16rpx;
}

.board-card {
  display: grid;
  gap: 12rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: #fff5ca;
}

.board-input {
  width: auto;
  border: 0;
  background: rgba(255, 255, 255, 0.56);
  border-radius: var(--mobile-radius-chip);
  padding: 16rpx;
}

.board-input--title {
  font-size: 28rpx;
  font-weight: 700;
}

.board-input--body {
  min-height: 140rpx;
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
</style>