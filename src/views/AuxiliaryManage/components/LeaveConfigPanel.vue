<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { LeaveConfigItem } from '@/types/auxiliary'
import { auxiliaryApi } from '@/api/modules/system/auxiliary'
import TemplateDownload from './common/TemplateDownload.vue'
import ExcelImport from './common/ExcelImport.vue'

const loading = ref(false)
const list = ref<LeaveConfigItem[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const form = reactive<Partial<LeaveConfigItem>>({ status: true })

const query = reactive({ pageNo: 1, pageSize: 10, userId: '', userName: '', startDate: '', endDate: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await auxiliaryApi.queryLeaveConfigs(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const save = async () => {
  await auxiliaryApi.saveLeaveConfig(form)
  ElMessage.success('请假配置保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async (id: string) => {
  await auxiliaryApi.deleteLeaveConfig(id)
  ElMessage.success('删除成功')
  loadData()
}

const onParsed = async (rows: Record<string, unknown>[]) => {
  await auxiliaryApi.importLeaveConfigs(rows as unknown as LeaveConfigItem[])
  ElMessage.success('导入成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header><div class="panel-title">请假配置</div></template>
    <el-form inline>
      <el-form-item label="人员ID"><el-input v-model="query.userId" /></el-form-item>
      <el-form-item label="人员名称"><el-input v-model="query.userName" /></el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      <el-form-item><el-button @click="dialogVisible = true">新增</el-button></el-form-item>
      <el-form-item><TemplateDownload name="leave-config" :columns="['userId','userName','leaveStart','leaveEnd','leaveReason','remark']" /></el-form-item>
      <el-form-item><ExcelImport @parsed="onParsed" /></el-form-item>
      <el-form-item><el-button @click="auxiliaryApi.exportLeaveConfigs(query)">导出</el-button></el-form-item>
    </el-form>

    <el-table :data="list" border v-loading="loading" row-key="id">
      <el-table-column prop="userId" label="人员ID" min-width="100" />
      <el-table-column prop="userName" label="人员名称" min-width="120" />
      <el-table-column prop="leaveStart" label="开始时间" min-width="170" />
      <el-table-column prop="leaveEnd" label="结束时间" min-width="170" />
      <el-table-column prop="leaveReason" label="请假原因" min-width="180" />
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" title="请假配置" width="520px">
      <el-form label-width="100px">
        <el-form-item label="人员ID"><el-input v-model="form.userId" /></el-form-item>
        <el-form-item label="人员名称"><el-input v-model="form.userName" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.leaveStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.leaveEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="请假原因"><el-input v-model="form.leaveReason" type="textarea" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.panel-title { font-size: 16px; font-weight: 700; }
.pager-wrap { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
