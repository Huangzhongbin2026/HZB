<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'

import { useWorkspaceStore } from '../stores/workspace'

const props = withDefaults(defineProps<{ embedded?: boolean }>(), {
  embedded: false,
})

const store = useWorkspaceStore()
const selectedProvider = ref('')
const { aiProviderCurrent, aiProviderForm, aiProviderPresets, aiProviderSaveStatus, errorMessage } = storeToRefs(store)

const currentPresetModels = computed(() => {
  const preset = aiProviderPresets.value.find((item) => item.provider_id === aiProviderForm.value.provider_id)
  return preset?.models ?? []
})

watch(
  () => aiProviderForm.value.provider_id,
  (value) => {
    selectedProvider.value = value
  },
  { immediate: true },
)

function onProviderChange() {
  store.applyAiProviderPreset(selectedProvider.value)
}
</script>

<template>
  <section :class="[props.embedded ? 'model-config-panel model-config-panel--embedded' : 'surface-card ai-studio-panel model-config-panel']">
    <header v-if="!props.embedded" class="surface-header model-config-panel__header">
      <div class="model-config-panel__heading">
        <h2>模型配置</h2>
      </div>
      <span class="status-pill">预设 {{ aiProviderPresets.length }}</span>
    </header>

    <div v-else class="model-config-panel__embedded-header">
      <div>
        <h3>模型配置</h3>
        <p class="helper-text">统一维护服务商、Base URL、模型和密钥。</p>
      </div>
      <span class="status-pill">预设 {{ aiProviderPresets.length }}</span>
    </div>

    <div class="sub-panel ai-provider-panel model-config-panel__body">
      <div class="ai-provider-grid model-config-panel__grid">
        <label class="model-config-panel__field">
          <span class="section-title">服务商预设（可选）</span>
          <select v-model="selectedProvider" class="field-input" @change="onProviderChange">
            <option v-for="preset in aiProviderPresets" :key="preset.provider_id" :value="preset.provider_id">
              {{ preset.label }}
            </option>
          </select>
          <span class="helper-text">用于快速填入常见服务商的 Base URL 和常用模型，例如 UniAPI、OpenAI、DeepSeek。</span>
        </label>
        <label class="model-config-panel__field">
          <span class="section-title">Base URL</span>
          <input v-model="aiProviderForm.base_url" class="field-input" placeholder="https://api.example.com/v1" />
        </label>
        <label class="model-config-panel__field">
          <span class="section-title">模型名</span>
          <input v-model="aiProviderForm.model_name" class="field-input" list="provider-models" placeholder="输入模型名" />
          <datalist id="provider-models">
            <option v-for="model in currentPresetModels" :key="model" :value="model" />
          </datalist>
        </label>
        <label class="model-config-panel__field">
          <span class="section-title">API Key</span>
          <input v-model="aiProviderForm.api_key" class="field-input" type="password" placeholder="输入新的 API Key" />
        </label>
      </div>
      <div class="ai-switch-row model-config-panel__actions">
        <button class="primary-button panel-top-action model-config-panel__save" type="button" @click="store.saveAiProviderConfig()">
          {{ aiProviderSaveStatus === 'saving' ? '保存中...' : '保存模型配置' }}
        </button>
      </div>
      <p class="helper-text model-config-panel__status">
        当前配置：{{ aiProviderCurrent?.provider_label || '未保存' }}
        <span v-if="aiProviderCurrent?.has_api_key"> · Key 已配置 {{ aiProviderCurrent.api_key_masked }}</span>
        <span v-if="aiProviderSaveStatus === 'saved'"> · 已保存</span>
        <span v-else-if="aiProviderSaveStatus === 'error'"> · 保存失败</span>
      </p>
      <p v-if="aiProviderSaveStatus === 'error' && errorMessage" class="helper-text helper-text--error">{{ errorMessage }}</p>
    </div>
  </section>
</template>