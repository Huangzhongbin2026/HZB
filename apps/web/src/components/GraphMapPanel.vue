<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()
const { currentNote, notes, tags } = storeToRefs(store)
const zoom = ref(1)

const graphNodes = computed(() => {
  const preferred = currentNote.value
    ? [currentNote.value, ...notes.value.filter((note) => note.slug !== currentNote.value?.slug)]
    : notes.value
  const visible = preferred.slice(0, 14)
  const palette = ['#8b7df0', '#d97757', '#2f7c6d', '#6f87d6', '#d1a436', '#9d6c93']

  return visible.map((note, index) => {
    const angle = (Math.PI * 2 * index) / Math.max(visible.length, 1)
    const isFocused = note.slug === currentNote.value?.slug
    const radius = isFocused ? 0 : 34 + (index % 3) * 9
    const x = isFocused ? 50 : 50 + Math.cos(angle) * radius
    const y = isFocused ? 50 : 50 + Math.sin(angle) * (radius * 0.72)
    const degree = visible.filter((item) => item.links.includes(note.slug) || note.links.includes(item.slug)).length

    return {
      ...note,
      x,
      y,
      size: 7 + Math.min(degree, 6) * 1.15 + Math.min(note.tags.length, 4) * 0.45,
      degree,
      color: palette[index % palette.length],
      isFocused,
    }
  })
})

const graphEdges = computed(() => {
  const nodeMap = new Map(graphNodes.value.map((node) => [node.slug, node]))
  const edgeKeys = new Set<string>()
  const edges: Array<{ key: string; x1: number; y1: number; x2: number; y2: number; emphasis: boolean }> = []

  for (const node of graphNodes.value) {
    for (const targetSlug of node.links) {
      const target = nodeMap.get(targetSlug)
      if (!target) {
        continue
      }
      const key = [node.slug, target.slug].sort().join('::')
      if (edgeKeys.has(key)) {
        continue
      }
      edgeKeys.add(key)
      edges.push({
        key,
        x1: node.x,
        y1: node.y,
        x2: target.x,
        y2: target.y,
        emphasis: node.slug === currentNote.value?.slug || target.slug === currentNote.value?.slug,
      })
    }
  }

  return edges
})

const denseNotes = computed(() =>
  [...graphNodes.value]
    .sort((left, right) => right.degree - left.degree)
    .slice(0, 5),
)

const graphStats = computed(() => ({
  nodes: notes.value.length,
  edges: notes.value.reduce((total, note) => total + note.links.length, 0),
  tags: tags.value.length,
}))

const graphTransform = computed(() => {
  const scale = zoom.value.toFixed(2)
  return `translate(50 50) scale(${scale}) translate(-50 -50)`
})

function openNote(slug: string) {
  router.push(`/notes/${slug}`)
}

function zoomIn() {
  zoom.value = Math.min(1.8, Number((zoom.value + 0.1).toFixed(2)))
}

function zoomOut() {
  zoom.value = Math.max(0.7, Number((zoom.value - 0.1).toFixed(2)))
}

function resetZoom() {
  zoom.value = 1
}

function handleWheel(event: WheelEvent) {
  if (event.deltaY < 0) {
    zoomIn()
    return
  }
  zoomOut()
}
</script>

<template>
  <section class="surface-card graph-panel">
    <header class="surface-header">
      <div>
        <h2>关系图谱</h2>
      </div>
      <div class="status-pill-row">
        <span class="status-pill">{{ graphStats.nodes }} 笔记</span>
        <span class="status-pill">{{ graphStats.edges }} 链接</span>
        <span class="status-pill">{{ graphStats.tags }} 标签</span>
        <button class="icon-button" type="button" title="缩小" aria-label="缩小" @click="zoomOut">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 12h10" /></svg>
        </button>
        <button class="icon-button" type="button" title="恢复" aria-label="恢复" @click="resetZoom">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 12a6 6 0 1 0 2-4.47" /><path d="M6 4v4h4" /></svg>
        </button>
        <button class="icon-button" type="button" title="放大" aria-label="放大" @click="zoomIn">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 7v10" /><path d="M7 12h10" /></svg>
        </button>
      </div>
    </header>

    <div class="graph-layout">
      <div class="graph-stage" @wheel.prevent="handleWheel">
        <svg class="graph-svg" viewBox="0 0 100 100" aria-label="knowledge graph">
          <g :transform="graphTransform">
            <line
              v-for="edge in graphEdges"
              :key="edge.key"
              class="graph-edge"
              :class="{ 'graph-edge--active': edge.emphasis }"
              :x1="edge.x1"
              :y1="edge.y1"
              :x2="edge.x2"
              :y2="edge.y2"
            />

            <g
              v-for="node in graphNodes"
              :key="node.slug"
              class="graph-node"
              :class="{ 'graph-node--focused': node.isFocused }"
              @click="openNote(node.slug)"
            >
              <circle :cx="node.x" :cy="node.y" :r="node.size / 2" :fill="node.color" fill-opacity="0.16" />
              <circle :cx="node.x" :cy="node.y" :r="Math.max(1.8, node.size / 5)" :fill="node.color" />
              <text :x="node.x" :y="node.y + node.size / 2 + 3" text-anchor="middle">{{ node.title }}</text>
            </g>
          </g>
        </svg>
      </div>

      <div class="graph-sidebar">
        <section class="sub-panel">
          <h3>当前焦点</h3>
          <p>
            {{ currentNote?.title ?? '从笔记页选择一篇笔记后，这里会高亮它在知识网络中的位置。' }}
          </p>
          <span class="helper-text">图谱默认展示最近活跃的 14 篇笔记，并按双向链接密度突出核心节点。</span>
        </section>

        <section class="sub-panel">
          <h3>高连接笔记</h3>
          <div class="reference-list">
            <button
              v-for="note in denseNotes"
              :key="note.slug"
              class="reference-card"
              type="button"
              @click="openNote(note.slug)"
            >
              <strong>{{ note.title }}</strong>
              <span>{{ note.path }}</span>
              <p>{{ note.degree }} 条关联，{{ note.tags.length }} 个标签</p>
            </button>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>