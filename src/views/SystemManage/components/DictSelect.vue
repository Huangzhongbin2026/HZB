<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { systemDictApi } from '@/api/modules/system/dict'
import type { SysDictItem } from '@/types/system'

const props = defineProps<{ modelValue?: string; typeCode: string; placeholder?: string }>()
const emits = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const options = ref<SysDictItem[]>([])
const value = ref(props.modelValue || '')

watch(
  () => props.modelValue,
  (val) => {
    value.value = val || ''
  },
)

const loadData = async () => {
  const res = await systemDictApi.listItems(props.typeCode)
  options.value = res.data
}

onMounted(loadData)
</script>

<template>
  <el-select v-model="value" :placeholder="placeholder || '请选择'" @change="(v) => emits('update:modelValue', String(v || ''))">
    <el-option v-for="item in options" :key="item.id" :label="item.itemName" :value="item.itemValue" />
  </el-select>
</template>
