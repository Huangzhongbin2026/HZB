<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { unorderedConsultApi } from '@/api/modules/unordered-consult'
import type { UnorderedManagementItem } from '@/types/supply-task/unordered-consult'

const loading = ref(false)
const tableData = ref<UnorderedManagementItem[]>([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  taskNo: '',
  crmNumber: '',
  productModel: '',
  createdAt: '',
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await unorderedConsultApi.queryManagement(query)
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const saveEvaluation = async (row: UnorderedManagementItem) => {
  await unorderedConsultApi.saveEvaluation(row.交期咨询任务编号, row.产品型号, row.统筹评估回复 || '', row.数量)
  ElMessage.success('评估回复已保存')
  loadData()
}

const urge = async (row: UnorderedManagementItem) => {
  await unorderedConsultApi.urge(row.交期咨询任务编号)
  ElMessage.success('已触发任务催办')
}

onMounted(loadData)
</script>

<template>
  <div class="manage-wrap">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item><el-input v-model="query.taskNo" placeholder="交期咨询任务编号" clearable /></el-form-item>
        <el-form-item><el-input v-model="query.crmNumber" placeholder="CRM编号" clearable /></el-form-item>
        <el-form-item><el-input v-model="query.productModel" placeholder="产品型号" clearable /></el-form-item>
        <el-form-item><el-input v-model="query.createdAt" placeholder="任务创建时间(模糊)" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="交期咨询任务编号" label="交期咨询任务编号" min-width="170" fixed="left" />
        <el-table-column prop="任务创建时间" label="任务创建时间" min-width="170" />
        <el-table-column prop="CRM编号" label="CRM编号" min-width="140" />
        <el-table-column prop="产品型号" label="产品型号" min-width="140" />
        <el-table-column prop="产品名称" label="产品名称" min-width="130" />
        <el-table-column prop="数量" label="数量" width="80" />
        <el-table-column prop="客户期望日期" label="客户期望日期" min-width="120" />
        <el-table-column prop="需求日期" label="需求日期" min-width="120" />
        <el-table-column prop="产品统筹" label="产品统筹" min-width="110" />
        <el-table-column prop="代理产品统筹" label="代理产品统筹" min-width="130" />
        <el-table-column prop="任务评估状态" label="任务评估状态" min-width="110" />
        <el-table-column prop="任务完成时效" label="任务完成时效(小时)" min-width="130" />
        <el-table-column label="统筹评估回复" min-width="220">
          <template #default="scope">
            <el-input
              v-model="scope.row.统筹评估回复"
              type="textarea"
              :rows="2"
              placeholder="请输入评估回复"
              @blur="saveEvaluation(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="任务完成时间" label="任务完成时间" min-width="170" />
        <el-table-column prop="任务修改时间" label="任务修改时间" min-width="170" />
        <el-table-column prop="最新评估时间" label="最新评估时间" min-width="160" />
        <el-table-column prop="最新评估数量" label="最新评估数量" min-width="120" />
        <el-table-column prop="最新评估统筹回复" label="最新评估统筹回复" min-width="180" />
        <el-table-column prop="倒数第二次评估时间" label="倒数第二次评估时间" min-width="170" />
        <el-table-column prop="倒数第二次评估数量" label="倒数第二次评估数量" min-width="130" />
        <el-table-column prop="倒数第二次评估统筹回复" label="倒数第二次评估统筹回复" min-width="190" />
        <el-table-column prop="倒数第三次评估时间" label="倒数第三次评估时间" min-width="170" />
        <el-table-column prop="倒数第三次评估数量" label="倒数第三次评估数量" min-width="130" />
        <el-table-column prop="倒数第三次评估统筹回复" label="倒数第三次评估统筹回复" min-width="190" />
        <el-table-column label="用户操作" min-width="110" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="urge(scope.row)">任务催办</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.manage-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card,
.table-card {
  border-radius: 12px;
}

.pager-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>
