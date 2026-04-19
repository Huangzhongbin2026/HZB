<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'

import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
const { currentNote, errorMessage, isInstanceAdmin, isTeamEdition, noteSharing, noteSharingStatus, selectableMembers } = storeToRefs(store)

const canEditSharing = computed(() => {
  return Boolean(currentNote.value && noteSharing.value && (isInstanceAdmin.value || noteSharing.value.owner_user_id === store.currentUser?.id))
})

function toggleMember(memberId: number) {
  if (!noteSharing.value) {
    return
  }
  const exists = noteSharing.value.shared_member_ids.includes(memberId)
  noteSharing.value = {
    ...noteSharing.value,
    shared_member_ids: exists
      ? noteSharing.value.shared_member_ids.filter((item) => item !== memberId)
      : [...noteSharing.value.shared_member_ids, memberId],
  }
}
</script>

<template>
  <section class="sub-panel sharing-panel">
    <div class="sharing-panel__header">
      <div>
        <p class="panel-eyebrow">Sharing</p>
        <h3>公开范围</h3>
      </div>
      <span class="status-pill">{{ isTeamEdition ? '团队版' : '个人版' }}</span>
    </div>

    <p v-if="!isTeamEdition" class="helper-text">个人版默认单用户隔离，公开范围配置只在团队版启用。</p>
    <template v-else-if="noteSharing">
      <label class="sharing-field">
        <span class="section-title">当前笔记对谁可见</span>
        <select v-model="noteSharing.visibility_scope" class="field-input" :disabled="!canEditSharing">
          <option value="private">仅自己与管理员</option>
          <option value="team">团队可见</option>
          <option value="selected">指定成员可见</option>
        </select>
      </label>

      <div v-if="noteSharing.visibility_scope === 'selected'" class="sharing-members">
        <label v-for="member in selectableMembers" :key="member.user_id" class="toggle-chip">
          <input :checked="noteSharing.shared_member_ids.includes(member.user_id)" type="checkbox" :disabled="!canEditSharing" @change="toggleMember(member.user_id)" />
          <span>{{ member.display_name }}</span>
        </label>
      </div>

      <div class="sharing-panel__actions">
        <button class="primary-button primary-button--small" type="button" :disabled="!canEditSharing" @click="store.saveNoteSharing()">
          {{ noteSharingStatus === 'saving' ? '保存中...' : '保存公开范围' }}
        </button>
        <p v-if="noteSharingStatus === 'saved'" class="helper-text">公开范围已更新。</p>
        <p v-if="noteSharingStatus === 'error' && errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
      </div>
    </template>
    <p v-else class="helper-text">当前笔记尚未加载分享策略。</p>
  </section>
</template>