<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { userOperationApi } from '@/api/modules/user-operation'

const loading = ref(false)
const tableData = ref<Array<Record<string, any>>>([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  requester: '',
  feishuId: '',
  flowType: '',
  status: '',
  createdAt: '',
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await userOperationApi.list(query)
    tableData.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="manage-wrap">
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item><el-input v-model="query.requester" placeholder="提问人" clearable /></el-form-item>
        <el-form-item><el-input v-model="query.feishuId" placeholder="飞书ID" clearable /></el-form-item>
        <el-form-item>
          <el-select v-model="query.flowType" placeholder="业务流" clearable style="width: 140px">
            <el-option label="订单加急" value="ORDER_URGENT" />
            <el-option label="未下单咨询" value="UNORDERED_ASSESS" />
            <el-option label="客期变更" value="DELIVERY_CHANGE" />
            <el-option label="智能问答" value="CHAT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
            <el-option label="进行中" value="processing" />
          </el-select>
        </el-form-item>
        <el-form-item><el-input v-model="query.createdAt" placeholder="时间(模糊)" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="opNo" label="操作编号" min-width="170" />
        <el-table-column prop="createdAt" label="操作时间" min-width="170" />
        <el-table-column prop="requester" label="提问人" min-width="100" />
        <el-table-column prop="feishuId" label="飞书ID" min-width="180" />
        <el-table-column prop="flowType" label="业务流" min-width="120" />
        <el-table-column prop="stepName" label="步骤" min-width="130" />
        <el-table-column prop="action" label="动作" min-width="120" />
        <el-table-column prop="status" label="状态" min-width="90" />
        <el-table-column label="操作数据" min-width="300">
          <template #default="scope">
            <el-input :model-value="JSON.stringify(scope.row.payload || {})" type="textarea" :rows="2" readonly />
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
