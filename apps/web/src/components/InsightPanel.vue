<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { aiPrompt, aiReferences, aiResponse, backlinks, currentNote, showAiPreview } = storeToRefs(store)
</script>

<template>
  <aside class="insight-panel">
    <section class="insight-card">
      <p class="panel-eyebrow">Backlinks</p>
      <h3>反向链接</h3>
      <ul>
        <li v-for="item in backlinks.incoming" :key="`${item.source_slug}-${item.target_slug}`">
          {{ item.source_slug }} -> {{ currentNote?.slug }}
        </li>
      </ul>
    </section>
    <section class="insight-card">
      <p class="panel-eyebrow">Outgoing</p>
      <h3>当前双链</h3>
      <ul>
        <li v-for="item in backlinks.outgoing" :key="`${item.source_slug}-${item.target_slug}`">[[{{ item.target_slug }}]]</li>
      </ul>
    </section>
    <section class="insight-card graph-card">
      <p class="panel-eyebrow">AI Assistant</p>
      <h3>RAG 创作</h3>
      <textarea v-model="aiPrompt" class="ai-textarea" placeholder="例如：总结当前笔记并补充关联知识" />
      <div class="ai-button-row">
        <button class="ghost-button ghost-button--small" type="button" @click="store.runAi('summary')">总结</button>
        <button class="ghost-button ghost-button--small" type="button" @click="store.runAi('expand')">扩写</button>
        <button class="ghost-button ghost-button--small" type="button" @click="store.runAi('polish')">润色</button>
        <button class="primary-button primary-button--small" type="button" @click="store.runAi('create', true)">创作并保存</button>
      </div>
      <pre class="ai-output">{{ aiResponse }}</pre>
      <button v-if="showAiPreview && aiResponse" class="primary-button primary-button--small" type="button" @click="store.applyAiResultToCurrentNote()">
        将 AI 结果追加到当前笔记
      </button>
      <div class="reference-list">
        <span v-for="item in aiReferences" :key="`${item.note_slug}-${item.score}`" class="reference-chip">
          {{ item.title }} · {{ item.score }}
        </span>
      </div>
    </section>
  </aside>
</template>
