<script setup lang="ts">
import type { TreeNode } from '../stores/workspace'

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
  <div class="tree-node">
    <button
      v-if="node.node_type === 'file'"
      class="tree-file tree-file--obsidian"
      :class="{ 'tree-file--active': activeSlug && node.note_slug === activeSlug }"
      type="button"
      @click="node.note_slug && emit('select', node.note_slug)"
    >
      <span class="tree-file__icon">•</span>
      <span class="tree-file__label">{{ node.name }}</span>
    </button>
    <details v-else class="tree-folder" open>
      <summary class="tree-folder-name">
        <span class="tree-folder-name__chevron">›</span>
        <span class="tree-folder-name__label">{{ node.name }}</span>
      </summary>
      <div class="tree-children">
        <TreeNode
          v-for="child in node.children"
          :key="child.path"
          :node="child"
          :active-slug="activeSlug"
          @select="emit('select', $event)"
        />
      </div>
    </details>
  </div>
</template>
