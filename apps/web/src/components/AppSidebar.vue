<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'

import InstanceSetupPanel from './InstanceSetupPanel.vue'
import ModelConfigPanel from './ModelConfigPanel.vue'
import SpaceManagerPanel from './SpaceManagerPanel.vue'
import TeamManagementPanel from './TeamManagementPanel.vue'
import { useWorkspaceStore } from '../stores/workspace'

const route = useRoute()
const store = useWorkspaceStore()
const { currentSpace, currentSpaceSlug, instanceConfig, isAuthenticated, isBusy, password, settingsModalOpen, settingsSection, spaces, syncStatus, teamInviteAcceptForm, teamInviteAcceptStatus, username } = storeToRefs(store)

const navItems = computed(() => [
  { to: '/', label: '首页', active: route.name === 'home' },
  { to: '/notes', label: '笔记', active: route.name === 'notes' },
  { to: '/graph', label: '图谱', active: route.name === 'graph' },
  { to: '/ai', label: '创作', active: route.name === 'ai' },
])
</script>

<template>
  <aside class="app-sidebar">
    <div class="sidebar-brand sidebar-brand--with-action">
      <div class="sidebar-brand__text">
        <h1 class="sidebar-title">{{ instanceConfig?.instance_name || '个人知识库' }}</h1>
        <span class="sidebar-subtitle">{{ instanceConfig?.edition === 'team' ? '团队协作 · 多空间' : '云端同步 · 双备份' }}</span>
      </div>
      <button class="sidebar-gear-button" type="button" aria-label="打开设置" @click="store.openSettings('instance')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M10.3 2.8h3.4l.5 2.2a7.6 7.6 0 0 1 1.8.8l2-1.1 2.4 2.4-1.1 2a7.6 7.6 0 0 1 .8 1.8l2.2.5v3.4l-2.2.5a7.6 7.6 0 0 1-.8 1.8l1.1 2-2.4 2.4-2-1.1a7.6 7.6 0 0 1-1.8.8l-.5 2.2h-3.4l-.5-2.2a7.6 7.6 0 0 1-1.8-.8l-2 1.1-2.4-2.4 1.1-2a7.6 7.6 0 0 1-.8-1.8l-2.2-.5v-3.4l2.2-.5a7.6 7.6 0 0 1 .8-1.8l-1.1-2 2.4-2.4 2 1.1a7.6 7.6 0 0 1 1.8-.8z" />
          <circle cx="12" cy="12" r="3.1" />
        </svg>
      </button>
    </div>

    <div v-if="!isAuthenticated && isBusy" class="auth-card auth-card--loading">
      <strong>正在连接本地知识库...</strong>
      <span class="sidebar-status-subtle">自动登录与工作区加载中</span>
    </div>

    <form v-else-if="!isAuthenticated" class="auth-card" @submit.prevent="store.login()">
      <input v-model="username" class="field-input" placeholder="账号" />
      <input v-model="password" class="field-input" type="password" placeholder="密码" />
      <button class="primary-button" type="submit" :disabled="isBusy">进入知识库</button>

      <div v-if="instanceConfig?.edition === 'team'" class="sidebar-invite-box">
        <strong>接受团队邀请</strong>
        <input v-model="teamInviteAcceptForm.invite_code" class="field-input" placeholder="邀请码或 invite code" />
        <input v-model="teamInviteAcceptForm.display_name" class="field-input" placeholder="显示名称" />
        <input v-model="teamInviteAcceptForm.username" class="field-input" placeholder="新账号用户名" />
        <input v-model="teamInviteAcceptForm.password" class="field-input" type="password" placeholder="新账号密码" />
        <button class="ghost-button" type="button" :disabled="teamInviteAcceptStatus === 'saving'" @click="store.acceptTeamInvite()">
          {{ teamInviteAcceptStatus === 'saving' ? '加入中...' : '接受邀请并登录' }}
        </button>
      </div>
    </form>

    <div v-else class="sidebar-status-card">
      <span class="sidebar-status-label">同步队列</span>
      <strong>{{ syncStatus.pending_events }} 条</strong>
      <span class="sidebar-status-subtle">桌面端 {{ syncStatus.connected_desktop_devices }} 台在线</span>
    </div>

    <div v-if="isAuthenticated && spaces.length > 0" class="sidebar-space-card">
      <span class="sidebar-status-label">当前空间</span>
      <select v-model="currentSpaceSlug" class="field-input" @change="store.setCurrentSpace(($event.target as HTMLSelectElement).value)">
        <option v-for="space in spaces" :key="space.slug" :value="space.slug">{{ space.name }}</option>
      </select>
      <span class="sidebar-status-subtle">{{ currentSpace?.visibility === 'team' ? '团队可见' : '私有空间' }} · {{ currentSpace?.slug || '未选择' }}</span>
    </div>

    <nav class="sidebar-nav-links">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="nav-link"
        :class="{ 'nav-link--active': item.active }"
      >
        <strong>{{ item.label }}</strong>
      </RouterLink>
    </nav>

    <div v-if="settingsModalOpen" class="modal-backdrop" @click.self="store.closeSettings()">
      <div class="settings-modal">
        <aside class="settings-modal__sidebar">
          <p class="panel-eyebrow">设置</p>
          <button class="settings-nav-item" :class="{ 'settings-nav-item--active': settingsSection === 'instance' }" type="button" @click="store.openSettings('instance')">实例与部署</button>
          <button class="settings-nav-item" :class="{ 'settings-nav-item--active': settingsSection === 'spaces' }" type="button" @click="store.openSettings('spaces')">知识空间</button>
          <button class="settings-nav-item" :class="{ 'settings-nav-item--active': settingsSection === 'team' }" type="button" @click="store.openSettings('team')">团队管理</button>
          <button class="settings-nav-item" :class="{ 'settings-nav-item--active': settingsSection === 'models' }" type="button" @click="store.openSettings('models')">模型服务</button>
        </aside>
        <section class="settings-modal__content">
          <header class="settings-modal__header">
            <div>
              <h2>系统设置</h2>
              <p class="helper-text">统一管理知识库的系统能力与连接配置。</p>
            </div>
            <button class="ghost-button ghost-button--small" type="button" @click="store.closeSettings()">关闭</button>
          </header>
          <InstanceSetupPanel v-if="settingsSection === 'instance'" />
          <SpaceManagerPanel v-else-if="settingsSection === 'spaces'" />
          <TeamManagementPanel v-else-if="settingsSection === 'team'" />
          <ModelConfigPanel v-else embedded />
        </section>
      </div>
    </div>
  </aside>
</template>
