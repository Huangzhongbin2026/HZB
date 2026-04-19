<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { errorMessage, instanceBootstrapForm, instanceBootstrapStatus, instanceConfig, isInstanceAdmin } = storeToRefs(store)
</script>

<template>
  <section class="settings-panel surface-card settings-panel--flat">
    <header class="surface-header settings-panel__header">
      <div>
        <h2>实例与部署</h2>
        <p class="helper-text">统一管理实例 ID 所属部署方式、个人版/团队版和授权状态。</p>
      </div>
      <span class="status-pill">{{ instanceConfig?.instance_id?.slice(0, 12) || '未初始化' }}</span>
    </header>

    <div class="settings-form-grid">
      <label class="settings-field">
        <span class="section-title">实例名称</span>
        <input v-model="instanceBootstrapForm.instance_name" class="field-input" placeholder="例如：张三的个人知识库" :disabled="!isInstanceAdmin" />
      </label>
      <label class="settings-field">
        <span class="section-title">部署方式</span>
        <select v-model="instanceBootstrapForm.deployment_mode" class="field-input" :disabled="!isInstanceAdmin">
          <option value="desktop">主电脑部署</option>
          <option value="server">服务器部署</option>
        </select>
      </label>
      <label class="settings-field">
        <span class="section-title">版本类型</span>
        <select v-model="instanceBootstrapForm.edition" class="field-input" :disabled="!isInstanceAdmin">
          <option value="personal">个人版</option>
          <option value="team">团队版</option>
        </select>
      </label>
      <label v-if="instanceBootstrapForm.edition === 'team'" class="settings-field">
        <span class="section-title">团队授权码</span>
        <input v-model="instanceBootstrapForm.authorization_code" class="field-input" type="password" placeholder="输入团队授权码" :disabled="!isInstanceAdmin" />
      </label>
    </div>

    <div class="settings-meta-grid">
      <div class="meta-card">
        <span>当前中心节点</span>
        <strong>{{ instanceConfig?.deployment_mode === 'server' ? '服务器' : '主电脑' }}</strong>
        <p>{{ instanceConfig?.deployment_mode === 'server' ? '服务器统一存储，主电脑保留本地副本。' : '主电脑在线时局域网与 Web 可围绕它同步。' }}</p>
      </div>
      <div class="meta-card">
        <span>商业形态</span>
        <strong>{{ instanceConfig?.edition === 'team' ? '团队版' : '个人版' }}</strong>
        <p>{{ instanceConfig?.edition === 'team' ? '支持多用户、专属空间、公开范围与团队 RAG。' : '个人可自由安装使用，全端同步与 AI 基础能力开放。' }}</p>
      </div>
      <div class="meta-card">
        <span>授权状态</span>
        <strong>{{ instanceConfig?.team_license_verified ? '已授权' : '未授权' }}</strong>
        <p>团队版需要授权码，个人版默认可用。</p>
      </div>
    </div>

    <div class="settings-actions">
      <button class="primary-button panel-top-action" type="button" :disabled="!isInstanceAdmin" @click="store.bootstrapInstance()">
        {{ instanceBootstrapStatus === 'saving' ? '保存中...' : '保存实例配置' }}
      </button>
      <p v-if="instanceBootstrapStatus === 'saved'" class="helper-text">实例配置已保存。</p>
      <p v-if="instanceBootstrapStatus === 'error' && errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
      <p v-if="!isInstanceAdmin" class="helper-text">只有管理员可以调整部署方式与版本类型。</p>
    </div>
  </section>
</template>