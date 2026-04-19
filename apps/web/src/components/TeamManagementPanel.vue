<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { errorMessage, instanceConfig, isInstanceAdmin, latestInvite, teamInviteAcceptForm, teamInviteAcceptStatus, teamInviteForm, teamInviteStatus, teamMembers } = storeToRefs(store)
</script>

<template>
  <section class="settings-panel surface-card settings-panel--flat">
    <header class="surface-header settings-panel__header">
      <div>
        <h2>团队管理</h2>
        <p class="helper-text">管理员可生成邀请链接，把成员拉入当前实例。成员公开后的内容才会进入团队搜索与 RAG。</p>
      </div>
      <span class="status-pill">{{ teamMembers.length }} 名成员</span>
    </header>

    <div v-if="instanceConfig?.edition !== 'team'" class="sub-panel">
      <p class="helper-text">当前实例还是个人版。切到团队版并通过授权后，才能启用团队邀请与成员公开协作。</p>
    </div>

    <template v-else>
      <div class="settings-form-grid settings-form-grid--two">
        <label class="settings-field">
          <span class="section-title">邀请角色</span>
          <select v-model="teamInviteForm.role" class="field-input" :disabled="!isInstanceAdmin">
            <option value="member">普通成员</option>
            <option value="manager">管理员助理</option>
          </select>
        </label>
        <label class="settings-field">
          <span class="section-title">有效期</span>
          <select v-model="teamInviteForm.expires_in_hours" class="field-input" :disabled="!isInstanceAdmin">
            <option :value="24">24 小时</option>
            <option :value="72">3 天</option>
            <option :value="168">7 天</option>
          </select>
        </label>
      </div>

      <div class="settings-actions">
        <button class="primary-button panel-top-action" type="button" :disabled="!isInstanceAdmin" @click="store.createTeamInvite()">
          {{ teamInviteStatus === 'saving' ? '生成中...' : '生成邀请链接' }}
        </button>
        <p v-if="teamInviteStatus === 'error' && errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
        <p v-if="!isInstanceAdmin" class="helper-text">只有管理员可以生成邀请链接。</p>
      </div>

      <div v-if="latestInvite" class="inline-action-card">
        <div class="inline-action-card__header">
          <strong>最近生成的邀请码</strong>
          <span class="status-pill">{{ latestInvite.status }}</span>
        </div>
        <div class="settings-invite-box">
          <code>{{ latestInvite.invite_link }}</code>
        </div>
      </div>

      <div class="settings-list">
        <article v-for="member in teamMembers" :key="member.user_id" class="settings-list-card">
          <div>
            <strong>{{ member.display_name }}</strong>
            <p>{{ member.username }} · {{ member.default_space_slug }}</p>
          </div>
          <div class="settings-list-meta">
            <span class="status-pill">{{ member.role }}</span>
            <span class="status-pill">{{ member.content_visibility_scope }}</span>
          </div>
        </article>
      </div>

      <div class="sub-panel">
        <h3>接受团队邀请</h3>
        <p class="helper-text">新成员可以在这里直接输入邀请码，创建账号并加入当前实例。</p>
        <div class="settings-form-grid settings-form-grid--two">
          <label class="settings-field settings-field--full">
            <span class="section-title">邀请码</span>
            <input v-model="teamInviteAcceptForm.invite_code" class="field-input" placeholder="粘贴 invite code 或链接里的 invite 参数" />
          </label>
          <label class="settings-field">
            <span class="section-title">显示名称</span>
            <input v-model="teamInviteAcceptForm.display_name" class="field-input" placeholder="例如 张三" />
          </label>
          <label class="settings-field">
            <span class="section-title">用户名</span>
            <input v-model="teamInviteAcceptForm.username" class="field-input" placeholder="登录用户名" />
          </label>
          <label class="settings-field settings-field--full">
            <span class="section-title">密码</span>
            <input v-model="teamInviteAcceptForm.password" class="field-input" type="password" placeholder="至少 8 位密码" />
          </label>
        </div>
        <div class="settings-actions">
          <button class="ghost-button panel-top-action" type="button" :disabled="teamInviteAcceptStatus === 'saving'" @click="store.acceptTeamInvite()">
            {{ teamInviteAcceptStatus === 'saving' ? '加入中...' : '接受邀请并登录' }}
          </button>
        </div>
      </div>
    </template>
  </section>
</template>