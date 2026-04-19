<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { createSpaceForm, createSpaceStatus, currentSpaceSlug, errorMessage, instanceConfig, spaces } = storeToRefs(store)
</script>

<template>
  <section class="settings-panel surface-card settings-panel--flat">
    <header class="surface-header settings-panel__header">
      <div>
        <h2>知识空间</h2>
        <p class="helper-text">每个成员都有默认专属空间，也可以继续拆分出不同用途的知识空间。</p>
      </div>
      <span class="status-pill">{{ spaces.length }} 个空间</span>
    </header>

    <div class="settings-form-grid settings-form-grid--three">
      <label class="settings-field">
        <span class="section-title">空间名称</span>
        <input v-model="createSpaceForm.name" class="field-input" placeholder="例如：团队产品库" />
      </label>
      <label class="settings-field">
        <span class="section-title">空间标识</span>
        <input v-model="createSpaceForm.slug" class="field-input" placeholder="例如：team-product" />
      </label>
      <label class="settings-field">
        <span class="section-title">可见范围</span>
        <select v-model="createSpaceForm.visibility" class="field-input">
          <option value="private">私有空间</option>
          <option value="team" :disabled="instanceConfig?.edition !== 'team'">团队空间</option>
        </select>
      </label>
    </div>

    <div class="settings-actions">
      <button class="primary-button panel-top-action" type="button" @click="store.createSpace()">
        {{ createSpaceStatus === 'saving' ? '创建中...' : '创建空间' }}
      </button>
      <p v-if="createSpaceStatus === 'saved'" class="helper-text">新空间已创建。</p>
      <p v-if="createSpaceStatus === 'error' && errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
    </div>

    <div class="settings-list">
      <article v-for="space in spaces" :key="space.slug" class="settings-list-card">
        <div>
          <strong>{{ space.name }}</strong>
          <p>{{ space.slug }} · {{ space.visibility === 'team' ? '团队公开' : '私有' }}</p>
        </div>
        <div class="settings-list-meta">
          <button v-if="currentSpaceSlug !== space.slug" class="ghost-button ghost-button--small" type="button" @click="store.setCurrentSpace(space.slug)">切换到此空间</button>
          <span class="status-pill">{{ currentSpaceSlug === space.slug ? '当前空间' : (space.is_default ? '默认空间' : '扩展空间') }}</span>
        </div>
      </article>
    </div>
  </section>
</template>