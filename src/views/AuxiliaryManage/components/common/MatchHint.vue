<script setup lang="ts">
defineProps<{ text: string; type?: 'success' | 'warning' | 'info' | 'error' }>()
</script>

<template>
  <el-alert :title="text" :type="type || 'info'" :closable="false" />
</template>
