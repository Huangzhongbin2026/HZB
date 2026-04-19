<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()
const { instanceConfig, recentVisitedNotes, sortedNotes, syncStatus, tags } = storeToRefs(store)

const recentNotes = computed(() => (recentVisitedNotes.value.length > 0 ? recentVisitedNotes.value.slice(0, 4) : sortedNotes.value.slice(0, 4)))

function openNote(slug: string) {
  router.push(`/notes/${slug}`)
}
</script>

<template>
  <section class="surface-card home-panel">
    <header class="surface-header">
      <div>
        <p class="panel-eyebrow">Overview</p>
        <h2>今日概览</h2>
      </div>
      <span class="status-pill">事件 {{ syncStatus.last_event_id ?? 0 }}</span>
    </header>

    <div class="overview-grid">
      <div class="overview-box">
        <span>最近活跃</span>
        <strong>{{ recentNotes.length }}</strong>
      </div>
      <div class="overview-box">
        <span>待同步</span>
        <strong>{{ syncStatus.pending_events }}</strong>
      </div>
      <div class="overview-box">
        <span>标签数</span>
        <strong>{{ tags.length }}</strong>
      </div>
      <div class="overview-box">
        <span>实例模式</span>
        <strong>{{ instanceConfig?.deployment_mode === 'server' ? '服务器' : '主电脑' }}</strong>
      </div>
      <div class="overview-box">
        <span>版本类型</span>
        <strong>{{ instanceConfig?.edition === 'team' ? '团队版' : '个人版' }}</strong>
      </div>
      <div class="overview-box">
        <span>授权状态</span>
        <strong>{{ instanceConfig?.team_license_verified ? '已授权' : '未授权' }}</strong>
      </div>
    </div>

    <div class="home-note-list">
      <button v-for="note in recentNotes" :key="note.slug" class="note-row" type="button" @click="openNote(note.slug)">
        <strong>{{ note.title }}</strong>
        <span>{{ note.path }}</span>
      </button>
    </div>
  </section>
</template>
