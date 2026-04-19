<template>
  <slot />
</template>

<style>
page {
  --mobile-bg: linear-gradient(180deg, #f6f2ea 0%, #ece7df 100%);
  --mobile-surface: rgba(255, 251, 245, 0.92);
  --mobile-surface-strong: #fffdf9;
  --mobile-border: rgba(118, 102, 85, 0.12);
  --mobile-text: #2d2a26;
  --mobile-muted: #756a5f;
  --mobile-accent: #4f6bff;
  --mobile-accent-soft: rgba(79, 107, 255, 0.12);
  --mobile-shadow: 0 10rpx 28rpx rgba(104, 84, 62, 0.08);
  --mobile-radius-card: 28rpx;
  --mobile-radius-control: 22rpx;
  --mobile-radius-chip: 18rpx;
  --mobile-space-page: 24rpx;
  --mobile-space-card: 26rpx;
  background: var(--mobile-bg);
  color: var(--mobile-text);
  font-family: 'Segoe UI', 'PingFang SC', sans-serif;
}

button {
  box-sizing: border-box;
}

view,
text,
input,
textarea,
scroll-view {
  box-sizing: border-box;
}
</style>
