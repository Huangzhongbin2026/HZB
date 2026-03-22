<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deliveryChangeApi } from '@/api/modules/delivery-change'
import type { DeliveryManagementItem } from '@/types/supply-task/delivery-change'

const loading = ref(false)
const tableData = ref<DeliveryManagementItem[]>([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  approvalNo: '',
  contractNo: '',
  taskType: '',
  createdAt: '',
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await deliveryChangeApi.queryManagement(query)
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const saveEvaluation = async (row: DeliveryManagementItem) => {
  await deliveryChangeApi.saveEvaluation(row.审批编号, String(row.市场代码名称 || ''), String(row.统筹评估回复 || ''))
  ElMessage.success('评估回复已保存')
  loadData()
}

const urgeUser = async (row: DeliveryManagementItem) => {
  await deliveryChangeApi.urgeUser(row.审批编号)
  ElMessage.success('已触发用户侧催办')
}

const urgeRegion = async (row: DeliveryManagementItem) => {
  await deliveryChangeApi.urgeRegion(row.审批编号)
  ElMessage.success('已触发区域统筹催办')
}

onMounted(loadData)
</script>

<template>
  <div class="manage-wrap">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item><el-input v-model="query.approvalNo" placeholder="审批编号" clearable /></el-form-item>
        <el-form-item><el-input v-model="query.contractNo" placeholder="合同编号" clearable /></el-form-item>
        <el-form-item>
          <el-select v-model="query.taskType" placeholder="任务类型" clearable style="width: 140px">
            <el-option label="客期提前" value="客期提前" />
            <el-option label="客期延后" value="客期延后" />
          </el-select>
        </el-form-item>
        <el-form-item><el-input v-model="query.createdAt" placeholder="任务创建时间(模糊)" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="审批编号" label="审批编号" min-width="170" fixed="left" />
        <el-table-column prop="任务创建时间" label="任务创建时间" min-width="170" />
        <el-table-column prop="任务类型" label="任务类型" min-width="110" />
        <el-table-column prop="合同编号" label="合同编号" min-width="140" />
        <el-table-column prop="市场代码名称" label="市场代码名称" min-width="130" />
        <el-table-column prop="物料描述" label="物料描述" min-width="160" />
        <el-table-column prop="未发货数量" label="未发货数量" width="100" />
        <el-table-column prop="变更数量" label="变更数量" width="90" />
        <el-table-column prop="客户期望日期" label="客户期望日期" min-width="120" />
        <el-table-column prop="客期提前至" label="客期提前至" min-width="120" />
        <el-table-column prop="客期延后至" label="客期延后至" min-width="120" />
        <el-table-column prop="产品统筹" label="产品统筹" min-width="100" />
        <el-table-column prop="代理产品统筹" label="代理产品统筹" min-width="120" />
        <el-table-column prop="区域统筹" label="区域统筹" min-width="110" />
        <el-table-column prop="代理区域统筹" label="代理区域统筹" min-width="120" />
        <el-table-column prop="任务评估状态" label="任务评估状态" min-width="110" />
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
        <el-table-column label="用户操作" min-width="110" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="urgeUser(scope.row)">任务催办</el-button>
          </template>
        </el-table-column>
        <el-table-column label="区域统筹操作" min-width="130" fixed="right">
          <template #default="scope">
            <el-button link type="warning" @click="urgeRegion(scope.row)">任务催办</el-button>
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
