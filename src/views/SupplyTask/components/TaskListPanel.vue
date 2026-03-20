<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { taskApi } from '@/api/modules/task'
import type { TaskItem } from '@/types/supply-task/task'

const loading = ref(false)
const tableData = ref<TaskItem[]>([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  type: '',
  status: '',
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await taskApi.queryTasks(query)
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never" class="panel-card">
    <template #header>
      <div class="panel-title">任务列表</div>
    </template>

    <el-form inline>
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="任务标题/产品型号" clearable />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.type" placeholder="任务类型" clearable style="width: 180px">
          <el-option label="已下单加急" value="ORDER_URGENT" />
          <el-option label="未下单交期评估" value="UNORDERED_ASSESS" />
          <el-option label="客期变更" value="DELIVERY_CHANGE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.status" placeholder="任务状态" clearable style="width: 180px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="待确认" value="WAIT_CONFIRM" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" row-key="id" border>
      <el-table-column prop="id" label="任务ID" min-width="180" />
      <el-table-column prop="type" label="类型" min-width="150" />
      <el-table-column prop="status" label="状态" min-width="120" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="productModel" label="产品型号" min-width="180" />
      <el-table-column prop="coordinator" label="统筹人" min-width="120" />
      <el-table-column prop="dueAt" label="应完成时间" min-width="180" />
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="loadData"
      />
    </div>
  </el-card>
</template>

<style scoped>
.panel-card {
  border-radius: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
