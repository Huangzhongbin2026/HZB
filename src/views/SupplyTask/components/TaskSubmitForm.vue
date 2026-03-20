<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { taskApi } from '@/api/modules/task'
import type { TaskSubmitDTO } from '@/types/supply-task/task'

const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<TaskSubmitDTO>({
  type: 'ORDER_URGENT',
  title: '',
  orderNo: '',
  productModel: '',
  requester: '',
  requiredDate: '',
  reason: '',
  priority: 3,
})

const rules: FormRules<TaskSubmitDTO> = {
  type: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  productModel: [{ required: true, message: '请输入产品型号', trigger: 'blur' }],
  requester: [{ required: true, message: '请输入提单人', trigger: 'blur' }],
  requiredDate: [{ required: true, message: '请选择需求日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入任务原因', trigger: 'blur' }],
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await taskApi.createTask(form)
    ElMessage.success(`任务已提交，任务ID: ${res.data.taskId}`)
    resetForm()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="panel-card">
    <template #header>
      <div class="panel-title">任务提交</div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="任务类型" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option label="已下单加急" value="ORDER_URGENT" />
              <el-option label="未下单交期评估" value="UNORDERED_ASSESS" />
              <el-option label="客期变更" value="DELIVERY_CHANGE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="优先级" prop="priority">
            <el-slider v-model="form.priority" :min="1" :max="5" :step="1" show-input />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="任务标题" prop="title">
            <el-input v-model="form.title" placeholder="示例：某客户订单加急" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="订单号" prop="orderNo">
            <el-input v-model="form.orderNo" placeholder="未下单评估可留空" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="产品型号" prop="productModel">
            <el-input v-model="form.productModel" placeholder="示例：RG-S6200-48XT8CQ" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="提单人" prop="requester">
            <el-input v-model="form.requester" placeholder="姓名/工号" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="需求日期" prop="requiredDate">
            <el-date-picker
              v-model="form.requiredDate"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="任务原因" prop="reason">
        <el-input v-model="form.reason" type="textarea" :rows="4" />
      </el-form-item>

      <el-space>
        <el-button type="primary" :loading="loading" @click="submit">提交任务</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-space>
    </el-form>
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
</style>
