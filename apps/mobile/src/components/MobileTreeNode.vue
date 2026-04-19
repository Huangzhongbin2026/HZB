<script setup lang="ts">
import type { TreeNode } from '../utils/mobileWorkspace'

withDefaults(
  defineProps<{
    node: TreeNode
    activeSlug?: string
  }>(),
  {
    activeSlug: '',
  },
)

const emit = defineEmits<{
  select: [slug: string]
}>()
</script>

<template>
  <view class="mobile-tree-node">
    <view v-if="node.node_type === 'file'" class="mobile-tree-file" :class="{ 'mobile-tree-file--active': activeSlug && node.note_slug === activeSlug }" @click="node.note_slug && emit('select', node.note_slug)">
      <text class="mobile-tree-file__dot">•</text>
      <text class="mobile-tree-file__label">{{ node.name }}</text>
    </view>
    <view v-else class="mobile-tree-folder">
      <view class="mobile-tree-folder__name">
        <text class="mobile-tree-folder__icon">▾</text>
        <text>{{ node.name }}</text>
      </view>
      <view class="mobile-tree-folder__children">
        <MobileTreeNode
          v-for="child in node.children"
          :key="child.path"
          :node="child"
          :active-slug="activeSlug"
          @select="emit('select', $event)"
        />
      </view>
    </view>
  </view>
</template>

<style scoped>
.mobile-tree-folder,
.mobile-tree-folder__children {
  display: grid;
  gap: 10rpx;
}

.mobile-tree-folder__name,
.mobile-tree-file {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 16rpx;
  border-radius: var(--mobile-radius-chip);
}

.mobile-tree-folder__name {
  color: var(--mobile-muted);
}

.mobile-tree-folder__children {
  padding-left: 22rpx;
}

.mobile-tree-file {
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid var(--mobile-border);
}

.mobile-tree-file--active {
  background: var(--mobile-accent-soft);
  border-color: rgba(79, 107, 255, 0.2);
}

.mobile-tree-file__dot,
.mobile-tree-folder__icon {
  color: var(--mobile-muted);
}

.mobile-tree-file__label {
  color: var(--mobile-text);
  font-size: 26rpx;
}
</style>