<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import NoteSharingPanel from './NoteSharingPanel.vue'
import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()
const { backlinks, currentNote } = storeToRefs(store)

function openNote(slug: string) {
  router.push(`/notes/${slug}`)
}
</script>

<template>
  <aside class="surface-card backlinks-panel">
    <section class="sub-panel">
      <p class="panel-eyebrow">Current</p>
      <h3>{{ currentNote?.title ?? '未选择笔记' }}</h3>
      <span class="helper-text">双向链接与反向链接只出现在编辑页，不在首页堆叠展示。</span>
    </section>

    <section class="sub-panel">
      <p class="panel-eyebrow">Outgoing</p>
      <ul class="link-list">
        <li v-for="item in backlinks.outgoing" :key="`${item.source_slug}-${item.target_slug}`">
          <button class="link-button" type="button" @click="openNote(item.target_slug)">[[{{ item.target_slug }}]]</button>
        </li>
      </ul>
    </section>

    <section class="sub-panel">
      <p class="panel-eyebrow">Backlinks</p>
      <ul class="link-list">
        <li v-for="item in backlinks.incoming" :key="`${item.source_slug}-${item.target_slug}`">
          <button class="link-button" type="button" @click="openNote(item.source_slug)">{{ item.source_slug }}</button>
        </li>
      </ul>
    </section>

    <NoteSharingPanel />
  </aside>
</template>
