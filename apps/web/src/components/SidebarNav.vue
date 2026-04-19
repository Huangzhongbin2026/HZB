<script setup lang="ts">
import { storeToRefs } from 'pinia'

import TreeNode from './TreeNode.vue'
import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { clipUrl, errorMessage, fileTree, isAuthenticated, isBusy, password, searchQuery, searchResults, syncStatus, tags, username } = storeToRefs(store)
</script>

<template>
  <aside class="sidebar-nav">
    <div class="sidebar-stack">
      <div>
        <p class="sidebar-caption">Workspace</p>
        <h1 class="sidebar-title">Knowledge Cloud</h1>
      </div>

      <form v-if="!isAuthenticated" class="auth-card" @submit.prevent="store.login()">
        <input v-model="username" class="field-input" placeholder="账号" />
        <input v-model="password" class="field-input" type="password" placeholder="密码" />
        <button class="primary-button" type="submit" :disabled="isBusy">登录</button>
      </form>

      <template v-else>
        <div class="sidebar-section">
          <div class="section-headline">
            <span>同步状态</span>
            <strong>{{ syncStatus.pending_events }} 待同步</strong>
          </div>
          <div class="status-pill-row">
            <span class="status-pill">桌面端 {{ syncStatus.connected_desktop_devices }}</span>
            <span class="status-pill">事件 {{ syncStatus.last_event_id ?? 0 }}</span>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="section-headline">
            <span>搜索</span>
            <button class="ghost-button ghost-button--small" type="button" @click="store.performSearch()">查找</button>
          </div>
          <input v-model="searchQuery" class="field-input" placeholder="全文检索与标签搜索" @keyup.enter="store.performSearch()" />
          <div class="search-results">
            <button
              v-for="item in searchResults"
              :key="item.note_slug"
              class="search-item"
              type="button"
              @click="store.selectNote(item.note_slug)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.snippet }}</span>
            </button>
          </div>
        </div>

        <div class="sidebar-section">
          <div class="section-headline">
            <span>网页剪藏</span>
            <button class="ghost-button ghost-button--small" type="button" @click="store.submitClip()">保存</button>
          </div>
          <input v-model="clipUrl" class="field-input" placeholder="粘贴 URL 抓取正文" @keyup.enter="store.submitClip()" />
        </div>

        <div class="sidebar-section">
          <div class="section-headline">
            <span>文件树</span>
            <button class="ghost-button ghost-button--small" type="button" @click="store.createQuickNote()">新建</button>
          </div>
          <div class="tree-shell">
            <TreeNode v-for="node in fileTree" :key="node.path" :node="node" @select="store.selectNote" />
          </div>
        </div>

        <div class="sidebar-section">
          <div class="section-headline">
            <span>标签</span>
          </div>
          <div class="tag-cloud">
            <span v-for="tag in tags" :key="tag.tag" class="tag-chip">#{{ tag.tag }} {{ tag.count }}</span>
          </div>
        </div>
      </template>
    </div>
    <div v-if="errorMessage" class="sidebar-footer sidebar-footer--warning">
      <span>异常</span>
      <strong>{{ errorMessage }}</strong>
    </div>
  </aside>
</template>
