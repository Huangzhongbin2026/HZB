<template>
  <view class="mobile-shell">
    <view class="mobile-shell__backdrop"></view>
    <view class="mobile-shell__content">
      <view class="mobile-shell__header">
        <view>
          <text class="mobile-shell__eyebrow">Knowledge Cloud</text>
          <text class="mobile-shell__title">{{ title }}</text>
          <text v-if="subtitle" class="mobile-shell__subtitle">{{ subtitle }}</text>
        </view>
        <view class="mobile-shell__header-actions">
          <button class="mobile-shell__icon-button" @click="goToCommand">⌘</button>
          <button v-if="showSettings" class="mobile-shell__icon-button" @click="goToSettings">⚙</button>
        </view>
      </view>

      <slot />
    </view>

    <view class="mobile-tabbar">
      <button
        v-for="item in navItems"
        :key="item.key"
        class="mobile-tabbar__item"
        :class="{ 'mobile-tabbar__item--active': current === item.key }"
        @click="navigate(item.path)"
      >
        <text class="mobile-tabbar__icon">{{ item.icon }}</text>
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  title: string
  subtitle?: string
  current: 'home' | 'notes' | 'graph' | 'ai' | 'daily' | 'templates' | 'whiteboard'
  showSettings?: boolean
}>()

const navItems = [
  { key: 'home', label: '首页', icon: '⌂', path: '/src/pages/index/index' },
  { key: 'notes', label: '笔记', icon: '▤', path: '/src/pages/notes/index' },
  { key: 'daily', label: '日记', icon: '◫', path: '/src/pages/daily/index' },
  { key: 'templates', label: '模板', icon: '≣', path: '/src/pages/templates/index' },
  { key: 'whiteboard', label: '白板', icon: '▧', path: '/src/pages/whiteboard/index' },
  { key: 'graph', label: '图谱', icon: '◎', path: '/src/pages/graph/index' },
  { key: 'ai', label: '创作', icon: '✦', path: '/src/pages/ai/index' },
]

function navigate(path: string) {
  const currentPages = getCurrentPages()
  const currentRoute = currentPages[currentPages.length - 1]?.route
  if (currentRoute === path.replace(/^\//, '')) {
    return
  }
  uni.reLaunch({ url: path })
}

function goToSettings() {
  uni.navigateTo({ url: '/src/pages/settings/index' })
}

function goToCommand() {
  uni.showActionSheet({
    itemList: ['新建笔记', '今日日记', '打开白板', '打开模板', '系统设置'],
    success(result) {
      if (result.tapIndex === 0) {
        uni.reLaunch({ url: '/src/pages/notes/index' })
        return
      }
      if (result.tapIndex === 1) {
        uni.reLaunch({ url: '/src/pages/daily/index' })
        return
      }
      if (result.tapIndex === 2) {
        uni.reLaunch({ url: '/src/pages/whiteboard/index' })
        return
      }
      if (result.tapIndex === 3) {
        uni.reLaunch({ url: '/src/pages/templates/index' })
        return
      }
      uni.navigateTo({ url: '/src/pages/settings/index' })
    },
  })
}
</script>

<style scoped>
.mobile-shell {
  position: relative;
  min-height: 100vh;
  padding: var(--mobile-space-page) var(--mobile-space-page) 188rpx;
  background: var(--mobile-bg);
}

.mobile-shell__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(186, 151, 104, 0.18), transparent 32%),
    radial-gradient(circle at 85% 18%, rgba(101, 137, 176, 0.14), transparent 24%);
  pointer-events: none;
}

.mobile-shell__content {
  position: relative;
  z-index: 1;
}

.mobile-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 28rpx;
}

.mobile-shell__header-actions {
  display: flex;
  gap: 12rpx;
}

.mobile-shell__eyebrow,
.mobile-shell__subtitle {
  display: block;
  color: var(--mobile-muted);
}

.mobile-shell__eyebrow {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  text-transform: uppercase;
}

.mobile-shell__title {
  display: block;
  margin-top: 8rpx;
  color: var(--mobile-text);
  font-size: 50rpx;
  font-weight: 700;
}

.mobile-shell__subtitle {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
}

.mobile-shell__icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 82rpx;
  height: 82rpx;
  margin: 0;
  padding: 0;
  border: 1px solid var(--mobile-border);
  border-radius: calc(var(--mobile-radius-control) + 4rpx);
  background: var(--mobile-surface);
  color: var(--mobile-text);
  font-size: 30rpx;
  line-height: 1;
}

.mobile-shell__icon-button::after,
.mobile-tabbar__item::after {
  border: 0;
}

.mobile-tabbar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 28rpx;
  z-index: 30;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 14rpx;
  padding: 14rpx;
  border: 1px solid var(--mobile-border);
  border-radius: 34rpx;
  background: rgba(255, 252, 247, 0.94);
  box-shadow: 0 18rpx 42rpx rgba(104, 84, 62, 0.12);
  backdrop-filter: blur(16rpx);
}

.mobile-tabbar__item {
  display: grid;
  justify-items: center;
  margin: 0;
  padding: 16rpx 0;
  border: 0;
  border-radius: 24rpx;
  background: transparent;
  color: var(--mobile-muted);
  line-height: 1;
}

.mobile-tabbar__item--active {
  background: linear-gradient(180deg, #3d342c, #241d18);
  color: #fff6ea;
}

.mobile-tabbar__icon {
  font-size: 28rpx;
  font-weight: 700;
}
</style>