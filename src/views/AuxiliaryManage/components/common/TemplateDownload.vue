<script setup lang="ts">
const props = defineProps<{ name: string; columns: string[] }>()

const downloadTemplate = () => {
  const csv = `${props.columns.join(',')}\n`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${props.name}-template.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}
</script>

<template>
  <el-button @click="downloadTemplate">下载模板</el-button>
</template>
