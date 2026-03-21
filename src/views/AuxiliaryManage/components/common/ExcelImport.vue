<script setup lang="ts">
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'

const emits = defineEmits<{ (e: 'parsed', rows: Record<string, unknown>[]): void }>()

const onUpload = (file: File) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    const data = e.target?.result
    if (!data) return
    const workbook = XLSX.read(data, { type: 'array' })
    const sheet = workbook.Sheets[workbook.SheetNames[0]]
    const rows = XLSX.utils.sheet_to_json<Record<string, unknown>>(sheet, { defval: '' })
    emits('parsed', rows)
    ElMessage.success(`解析完成，共 ${rows.length} 行`)
  }
  reader.readAsArrayBuffer(file)
  return false
}
</script>

<template>
  <el-upload :show-file-list="false" :before-upload="onUpload">
    <el-button>Excel导入</el-button>
  </el-upload>
</template>
