<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { sortedNotes } = storeToRefs(store)
</script>

<template>
  <section class="note-list-panel">
    <header class="panel-header">
      <div>
        <p class="panel-eyebrow">Knowledge Vault</p>
        <h2>最近活跃</h2>
      </div>
      <button class="ghost-button" type="button" @click="store.createQuickNote()">新建</button>
    </header>
    <div class="note-cards">
      <article v-for="note in sortedNotes" :key="note.id" class="note-card" @click="store.selectNote(note.slug)">
        <h3>{{ note.title }}</h3>
        <p>{{ note.summary }}</p>
        <span>{{ note.path }}</span>
      </article>
    </div>
  </section>
</template>
