<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'

import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const store = useWorkspaceStore()
const { searchQuery, searchResults } = storeToRefs(store)

function openNote(slug: string) {
  router.push(`/notes/${slug}`)
}
</script>

<template>
  <section class="surface-card home-panel">
    <header class="surface-header">
      <div>
        <p class="panel-eyebrow">Search</p>
        <h2>搜索你的知识库</h2>
      </div>
      <button class="ghost-button panel-top-action" type="button" @click="store.performSearch()">搜索</button>
    </header>

    <input
      v-model="searchQuery"
      class="field-input field-input--large"
      placeholder="搜索标题、正文、标签或关键术语"
      @keyup.enter="store.performSearch()"
    />

    <div class="home-result-list">
      <button
        v-for="item in searchResults"
        :key="item.note_slug"
        class="search-item"
        type="button"
        @click="openNote(item.note_slug)"
      >
        <strong>{{ item.title }}</strong>
        <span>{{ item.path }}</span>
        <p>{{ item.snippet }}</p>
      </button>
    </div>
  </section>
</template>
