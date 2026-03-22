<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderUrgentApi } from '@/api/modules/order-urgent'
import type { OrderUrgentManagementItem } from '@/types/supply-task/order-urgent'

const loading = ref(false)
const tableData = ref<OrderUrgentManagementItem[]>([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  contractNo: '',
  orderNo: '',
  projectName: '',
  createdAt: '',
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await orderUrgentApi.queryManagement(query)
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const saveEvaluation = async (row: OrderUrgentManagementItem) => {
  await orderUrgentApi.saveEvaluation(row.加急任务编号, row.统筹评估回复 || '')
  ElMessage.success('评估回复已保存')
  loadData()
}

const urgeUser = async (row: OrderUrgentManagementItem) => {
  await orderUrgentApi.urgeUser(row.加急任务编号)
  ElMessage.success('已触发用户侧催办')
}

const urgeRegion = async (row: OrderUrgentManagementItem) => {
  await orderUrgentApi.urgeRegion(row.加急任务编号)
  ElMessage.success('已触发区域统筹催办')
}

onMounted(loadData)
</script>

<template>
  <div class="right-main">
      <el-card shadow="never" class="filter-card">
        <el-form inline>
          <el-form-item>
            <el-input v-model="query.contractNo" placeholder="合同编号" clearable />
          </el-form-item>
          <el-form-item>
            <el-input v-model="query.orderNo" placeholder="订单编号" clearable />
          </el-form-item>
          <el-form-item>
            <el-input v-model="query.projectName" placeholder="项目名称" clearable />
          </el-form-item>
          <el-form-item>
            <el-input v-model="query.createdAt" placeholder="创建时间(模糊)" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadData">查询</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="table-card">
        <el-table :data="tableData" v-loading="loading" border>
          <el-table-column prop="加急任务编号" label="加急任务编号" min-width="170" fixed="left" />
          <el-table-column prop="合同编号" label="合同编号" min-width="140" />
          <el-table-column prop="订单编号" label="订单编号" min-width="130" />
          <el-table-column prop="项目名称" label="项目名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="任务提交人" label="任务提交人" min-width="120" />
          <el-table-column prop="区域统筹" label="区域统筹" min-width="120" />
          <el-table-column prop="代理区域统筹" label="代理区域统筹" min-width="130" />
          <el-table-column prop="代理产品统筹" label="代理产品统筹" min-width="130" />
          <el-table-column prop="任务创建时间" label="任务创建时间" min-width="170" />
          <el-table-column prop="任务评估状态" label="任务评估状态" min-width="120" />
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
          <el-table-column label="用户操作" min-width="120" fixed="right">
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
.right-main {
  padding: 0;
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
