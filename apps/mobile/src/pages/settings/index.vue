<template>
  <MobileShell current="settings" title="设置" subtitle="把外观、快捷键、实例与模型配置收进同一块控制台。" :show-settings="true">
    <view class="overview-grid">
      <view class="overview-card overview-card--dark">
        <text class="overview-label">界面</text>
        <text class="overview-value">{{ appearanceThemeLabel }}</text>
        <text class="overview-meta">{{ appearanceDensityLabel }} · {{ sidebarBehaviorLabel }}</text>
      </view>
      <view class="overview-card">
        <text class="overview-label">实例</text>
        <text class="overview-value">{{ state.instanceConfig?.instance_name || '未命名实例' }}</text>
        <text class="overview-meta">{{ state.instanceConfig?.deployment_mode === 'server' ? '服务器部署' : '主电脑部署' }}</text>
      </view>
      <view class="overview-card">
        <text class="overview-label">模型</text>
        <text class="overview-value">{{ state.aiProviderCurrent?.model_name || '未配置' }}</text>
        <text class="overview-meta">{{ state.aiProviderCurrent?.provider_label || '等待设置' }}</text>
      </view>
      <view class="overview-card">
        <text class="overview-label">快捷键</text>
        <text class="overview-value">{{ shortcutRows.length }} 组</text>
        <text class="overview-meta">命令面板、搜索、白板、日记</text>
      </view>
    </view>

    <view class="section-card form-card">
      <view class="section-head">
        <text class="section-title">外观与侧栏</text>
        <text class="section-desc">参考 Obsidian，把菜单与扩展栏行为做成可调节项。</text>
      </view>
      <picker :range="themeOptions" :value="themeIndex" @change="handleThemeChange">
        <view class="picker-field">{{ appearanceThemeLabel }}</view>
      </picker>
      <picker :range="densityOptions" :value="densityIndex" @change="handleDensityChange">
        <view class="picker-field">{{ appearanceDensityLabel }}</view>
      </picker>
      <picker :range="sidebarOptions" :value="sidebarIndex" @change="handleSidebarChange">
        <view class="picker-field">{{ sidebarBehaviorLabel }}</view>
      </picker>
      <button class="secondary-button" @click="saveAppearance">保存外观偏好</button>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">快捷键与命令</text>
        <text class="section-desc">手机端展示命令面板映射，方便跨端统一记忆。</text>
      </view>
      <view v-for="item in shortcutRows" :key="item.label" class="list-card">
        <text class="list-card__title">{{ item.label }}</text>
        <text class="detail-line">{{ item.value }}</text>
      </view>
    </view>

    <view class="section-card form-card">
      <text class="section-title">实例与部署</text>
      <input v-model="state.instanceBootstrapForm.instance_name" class="field-input" placeholder="实例名称" :disabled="!isAdmin" />
      <picker :range="deploymentModeLabels" :value="deploymentModeIndex" @change="handleDeploymentChange">
        <view class="picker-field">{{ deploymentModeLabel }}</view>
      </picker>
      <picker :range="editionLabels" :value="editionIndex" @change="handleEditionChange">
        <view class="picker-field">{{ editionLabel }}</view>
      </picker>
      <input
        v-if="state.instanceBootstrapForm.edition === 'team'"
        v-model="state.instanceBootstrapForm.authorization_code"
        class="field-input"
        password
        placeholder="团队授权码"
        :disabled="!isAdmin"
      />
      <button class="primary-button" :loading="state.instanceBootstrapStatus === 'saving'" :disabled="!isAdmin" @click="saveInstanceConfig">保存实例配置</button>
      <text class="status-text">{{ instanceStatusText }}</text>
    </view>

    <view class="section-card form-card">
      <text class="section-title">知识空间</text>
      <picker :range="spaceOptions" :value="currentSpaceIndex" @change="handleCurrentSpaceChange">
        <view class="picker-field">{{ currentSpaceLabel }}</view>
      </picker>
      <input v-model="state.createSpaceForm.name" class="field-input" placeholder="空间名称" />
      <input v-model="state.createSpaceForm.slug" class="field-input" placeholder="空间标识" />
      <picker :range="spaceVisibilityLabels" :value="spaceVisibilityIndex" @change="handleSpaceVisibilityChange">
        <view class="picker-field">{{ spaceVisibilityLabel }}</view>
      </picker>
      <button class="primary-button" :loading="state.createSpaceStatus === 'saving'" @click="saveSpace">创建空间</button>
      <view v-for="space in state.spaces" :key="space.slug" class="list-card">
        <text class="list-card__title">{{ space.name }}</text>
        <text class="detail-line">{{ space.slug }} · {{ space.is_default ? '默认空间' : '扩展空间' }} · {{ space.visibility === 'team' ? '团队可见' : '私有' }}</text>
      </view>
    </view>

    <view class="section-card form-card">
      <text class="section-title">团队管理</text>
      <text class="status-text">{{ teamStatusText }}</text>
      <template v-if="state.instanceConfig?.edition === 'team'">
        <picker :range="inviteRoleLabels" :value="inviteRoleIndex" @change="handleInviteRoleChange">
          <view class="picker-field">{{ inviteRoleLabel }}</view>
        </picker>
        <picker :range="inviteExpiryLabels" :value="inviteExpiryIndex" @change="handleInviteExpiryChange">
          <view class="picker-field">{{ inviteExpiryLabel }}</view>
        </picker>
        <button class="primary-button" :loading="state.teamInviteStatus === 'saving'" :disabled="!isAdmin" @click="generateInvite">生成邀请链接</button>
        <text v-if="state.latestInvite" class="detail-line">最近邀请：{{ state.latestInvite.invite_link }}</text>
        <view v-for="member in state.teamMembers" :key="member.user_id" class="list-card">
          <text class="list-card__title">{{ member.display_name }}</text>
          <text class="detail-line">{{ member.username }} · {{ member.role }} · {{ member.default_space_slug }}</text>
        </view>

        <text class="section-title section-title--sub">接受团队邀请</text>
        <input v-model="state.teamInviteAcceptForm.invite_code" class="field-input" placeholder="邀请码或 invite code" />
        <input v-model="state.teamInviteAcceptForm.display_name" class="field-input" placeholder="显示名称" />
        <input v-model="state.teamInviteAcceptForm.username" class="field-input" placeholder="登录用户名" />
        <input v-model="state.teamInviteAcceptForm.password" class="field-input" password placeholder="登录密码" />
        <button class="secondary-button" :loading="state.teamInviteAcceptStatus === 'saving'" @click="joinTeam">接受邀请并登录</button>
      </template>
    </view>

    <view class="section-card form-card">
      <text class="section-title">模型预设</text>
      <picker :range="presetLabels" @change="handlePresetChange">
        <view class="picker-field">{{ currentPresetLabel }}</view>
      </picker>
      <input v-model="state.aiProviderForm.provider_label" class="field-input" placeholder="显示名称" />
      <input v-model="state.aiProviderForm.base_url" class="field-input" placeholder="Base URL" />
      <input v-model="state.aiProviderForm.model_name" class="field-input" placeholder="模型名称" />
      <input v-model="state.aiProviderForm.api_key" class="field-input" password placeholder="API Key" />
      <button class="primary-button" :loading="state.aiProviderSaveStatus === 'saving'" @click="saveConfig">保存模型配置</button>
      <text class="status-text">{{ saveStatusText }}</text>
    </view>

    <view class="section-card">
      <text class="section-title">当前配置</text>
      <text class="detail-line">服务商：{{ state.aiProviderCurrent?.provider_label || '未配置' }}</text>
      <text class="detail-line">模型：{{ state.aiProviderCurrent?.model_name || '未配置' }}</text>
      <text class="detail-line">地址：{{ state.aiProviderCurrent?.base_url || '未配置' }}</text>
      <text class="detail-line">密钥：{{ state.aiProviderCurrent?.api_key_masked || '未保存' }}</text>
    </view>
  </MobileShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

import MobileShell from '../../components/MobileShell.vue'
import {
  applyProviderPreset,
  bootstrapInstanceConfig,
  bootstrapMobileWorkspace,
  createSpace,
  createTeamInvite,
  acceptTeamInvite,
  loadInstanceConfig,
  loadSpaces,
  loadTeamMembers,
  mobileWorkspace,
  saveAiProviderConfig,
  setCurrentSpace,
} from '../../utils/mobileWorkspace'

const state = mobileWorkspace
const deploymentModeLabels = ['主电脑部署', '服务器部署']
const editionLabels = ['个人版', '团队版']
const spaceVisibilityLabels = ['私有空间', '团队空间']
const inviteRoleLabels = ['普通成员', '管理员助理']
const inviteExpiryLabels = ['24 小时', '3 天', '7 天']
const themeOptions = ['羊皮纸暖色', '石墨深灰', '浅雾米白']
const densityOptions = ['舒展布局', '紧凑布局']
const sidebarOptions = ['默认展开双栏', '进入笔记时自动收起目录', '只保留左侧目录']
const shortcutRows = [
  { label: '命令面板', value: 'Ctrl/Cmd + K' },
  { label: '全局搜索', value: 'Ctrl/Cmd + O' },
  { label: '新建笔记', value: 'Ctrl/Cmd + N' },
  { label: '打开白板', value: 'Ctrl/Cmd + Shift + B' },
  { label: '今日日记', value: 'Ctrl/Cmd + D' },
]

const appearanceTheme = ref(uni.getStorageSync('mobile-appearance-theme') || themeOptions[0])
const appearanceDensity = ref(uni.getStorageSync('mobile-appearance-density') || densityOptions[0])
const sidebarBehavior = ref(uni.getStorageSync('mobile-sidebar-behavior') || sidebarOptions[0])

const isAdmin = computed(() => state.currentUser?.role === 'admin')
const deploymentModeIndex = computed(() => (state.instanceBootstrapForm.deployment_mode === 'server' ? 1 : 0))
const deploymentModeLabel = computed(() => deploymentModeLabels[deploymentModeIndex.value])
const editionIndex = computed(() => (state.instanceBootstrapForm.edition === 'team' ? 1 : 0))
const editionLabel = computed(() => editionLabels[editionIndex.value])
const spaceVisibilityIndex = computed(() => (state.createSpaceForm.visibility === 'team' ? 1 : 0))
const spaceVisibilityLabel = computed(() => spaceVisibilityLabels[spaceVisibilityIndex.value])
const spaceOptions = computed(() => state.spaces.map((space) => `${space.name} · ${space.visibility === 'team' ? '团队' : '私有'}`))
const currentSpaceIndex = computed(() => Math.max(state.spaces.findIndex((space) => space.slug === state.currentSpaceSlug), 0))
const currentSpaceLabel = computed(() => spaceOptions.value[currentSpaceIndex.value] || '选择当前空间')
const inviteRoleIndex = computed(() => (state.teamInviteForm.role === 'manager' ? 1 : 0))
const inviteRoleLabel = computed(() => inviteRoleLabels[inviteRoleIndex.value])
const inviteExpiryIndex = computed(() => {
  if (state.teamInviteForm.expires_in_hours === 24) return 0
  if (state.teamInviteForm.expires_in_hours === 168) return 2
  return 1
})
const inviteExpiryLabel = computed(() => inviteExpiryLabels[inviteExpiryIndex.value])
const themeIndex = computed(() => Math.max(themeOptions.indexOf(appearanceTheme.value), 0))
const densityIndex = computed(() => Math.max(densityOptions.indexOf(appearanceDensity.value), 0))
const sidebarIndex = computed(() => Math.max(sidebarOptions.indexOf(sidebarBehavior.value), 0))
const appearanceThemeLabel = computed(() => themeOptions[themeIndex.value])
const appearanceDensityLabel = computed(() => densityOptions[densityIndex.value])
const sidebarBehaviorLabel = computed(() => sidebarOptions[sidebarIndex.value])

const presetLabels = computed(() => state.aiProviderPresets.map((item) => item.label))
const currentPresetLabel = computed(() => {
  return state.aiProviderPresets.find((item) => item.provider_id === state.aiProviderForm.provider_id)?.label || '选择模型预设'
})
const instanceStatusText = computed(() => {
  if (state.instanceBootstrapStatus === 'saved') {
    return '实例配置已保存，新的部署模式和版本类型已生效。'
  }
  if (state.instanceBootstrapStatus === 'error') {
    return state.errorMessage || '实例配置保存失败。'
  }
  return `当前实例：${state.instanceConfig?.instance_name || '未初始化'} · ${state.instanceConfig?.deployment_mode === 'server' ? '服务器' : '主电脑'} · ${state.instanceConfig?.edition === 'team' ? '团队版' : '个人版'}`
})
const teamStatusText = computed(() => {
  if (state.instanceConfig?.edition !== 'team') {
    return '当前实例是个人版，团队邀请与共享协作未开启。'
  }
  return state.instanceConfig.team_license_verified ? '团队版已授权，可生成邀请链接并管理团队成员。' : '团队版尚未授权。'
})
const saveStatusText = computed(() => {
  if (state.aiProviderSaveStatus === 'saved') {
    return '已保存，移动端会继续沿用网页端同一套模型配置。'
  }
  if (state.aiProviderSaveStatus === 'error') {
    return '保存失败，请检查 Base URL、模型名或 API Key。'
  }
  return '支持主流 OpenAI 兼容模型与 UniAPI。'
})

onShow(async () => {
  await bootstrapMobileWorkspace()
  await loadInstanceConfig()
  await loadSpaces()
  await loadTeamMembers()
})

function handleDeploymentChange(event: any) {
  state.instanceBootstrapForm.deployment_mode = Number(event.detail.value) === 1 ? 'server' : 'desktop'
}

function handleEditionChange(event: any) {
  state.instanceBootstrapForm.edition = Number(event.detail.value) === 1 ? 'team' : 'personal'
}

function handleSpaceVisibilityChange(event: any) {
  state.createSpaceForm.visibility = Number(event.detail.value) === 1 ? 'team' : 'private'
}

function handleCurrentSpaceChange(event: any) {
  const space = state.spaces[Number(event.detail.value)]
  if (space) {
    setCurrentSpace(space.slug)
  }
}

function handleInviteRoleChange(event: any) {
  state.teamInviteForm.role = Number(event.detail.value) === 1 ? 'manager' : 'member'
}

function handleInviteExpiryChange(event: any) {
  state.teamInviteForm.expires_in_hours = [24, 72, 168][Number(event.detail.value)] || 72
}

function handleThemeChange(event: any) {
  appearanceTheme.value = themeOptions[Number(event.detail.value)] || themeOptions[0]
}

function handleDensityChange(event: any) {
  appearanceDensity.value = densityOptions[Number(event.detail.value)] || densityOptions[0]
}

function handleSidebarChange(event: any) {
  sidebarBehavior.value = sidebarOptions[Number(event.detail.value)] || sidebarOptions[0]
}

function handlePresetChange(event: any) {
  const index = Number(event.detail.value)
  const preset = state.aiProviderPresets[index]
  if (preset) {
    applyProviderPreset(preset.provider_id)
  }
}

async function saveInstanceConfig() {
  await bootstrapInstanceConfig()
}

async function saveSpace() {
  await createSpace()
}

async function generateInvite() {
  await createTeamInvite()
}

async function joinTeam() {
  await acceptTeamInvite()
}

async function saveConfig() {
  await saveAiProviderConfig()
}

function saveAppearance() {
  uni.setStorageSync('mobile-appearance-theme', appearanceTheme.value)
  uni.setStorageSync('mobile-appearance-density', appearanceDensity.value)
  uni.setStorageSync('mobile-sidebar-behavior', sidebarBehavior.value)
  uni.showToast({ title: '已保存', icon: 'success' })
}
</script>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
  margin-bottom: 22rpx;
}

.overview-card {
  display: grid;
  gap: 10rpx;
  padding: 24rpx;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-card);
  background: var(--mobile-surface);
  box-shadow: var(--mobile-shadow);
}

.overview-card--dark {
  background: linear-gradient(180deg, #3c342d, #211c18);
}

.overview-card--dark .overview-label,
.overview-card--dark .overview-value,
.overview-card--dark .overview-meta {
  color: #fff3e4;
}

.overview-label,
.overview-meta,
.status-text,
.detail-line,
.section-desc {
  display: block;
  color: var(--mobile-muted);
}

.overview-label,
.overview-meta,
.section-desc,
.status-text,
.detail-line {
  font-size: 24rpx;
  line-height: 1.7;
}

.overview-value {
  display: block;
  color: var(--mobile-text);
  font-size: 36rpx;
  font-weight: 700;
}

.list-card {
  padding: 18rpx 20rpx;
  border-radius: var(--mobile-radius-control);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--mobile-border);
}

.list-card__title {
  display: block;
  color: var(--mobile-text);
  font-size: 28rpx;
  font-weight: 700;
}

.section-title {
  display: block;
  color: var(--mobile-text);
  font-size: 34rpx;
  font-weight: 700;
}

.section-card {
  margin-bottom: 22rpx;
  padding: var(--mobile-space-card);
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-card);
  background: var(--mobile-surface);
  box-shadow: var(--mobile-shadow);
}

.form-card {
  display: grid;
  gap: 16rpx;
}

.section-head {
  margin-bottom: 8rpx;
}

.picker-field,
.field-input {
  width: auto;
  min-height: 86rpx;
  padding: 0 24rpx;
  border: 1px solid var(--mobile-border);
  border-radius: var(--mobile-radius-control);
  background: var(--mobile-surface-strong);
  color: var(--mobile-text);
  font-size: 28rpx;
  line-height: 86rpx;
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 88rpx;
  margin: 0;
  padding: 0 28rpx;
  border-radius: 22rpx;
  font-size: 28rpx;
}

.primary-button {
  border: 0;
  background: linear-gradient(180deg, #2f2924, #1d1814);
  color: #fff8ef;
}

.secondary-button {
  border: 1px solid var(--mobile-border);
  background: rgba(255, 252, 247, 0.86);
  color: var(--mobile-text);
}

.primary-button::after,
.secondary-button::after {
  border: 0;
}
</style>