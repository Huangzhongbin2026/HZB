<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi, type LeaveConfig, type MessageSwitchConfig } from '@/api/modules/config'

const leaveList = ref<LeaveConfig[]>([])
const loading = ref(false)

const newLeave = reactive<LeaveConfig>({
  userId: '',
  startAt: '',
  endAt: '',
  agentUserId: '',
  productModels: [],
})

const messageSwitch = reactive<MessageSwitchConfig>({
  taskCreated: true,
  taskTransfer: true,
  taskOverdue: true,
})

const loadConfig = async () => {
  loading.value = true
  try {
    const leaveRes = await configApi.getLeaveConfigs()
    leaveList.value = leaveRes.data

    const switchRes = await configApi.getMessageSwitch()
    Object.assign(messageSwitch, switchRes.data)
  } finally {
    loading.value = false
  }
}

const saveLeave = async () => {
  await configApi.saveLeaveConfig({ ...newLeave })
  ElMessage.success('请假代理配置已保存')
  loadConfig()
}

const saveSwitch = async () => {
  await configApi.saveMessageSwitch({ ...messageSwitch })
  ElMessage.success('消息开关已保存')
}

onMounted(loadConfig)
</script>

<template>
  <div class="config-layout" v-loading="loading">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-title">请假代理产品型号配置</div>
      </template>
      <el-form label-width="130px">
        <el-form-item label="请假人">
          <el-input v-model="newLeave.userId" placeholder="工号" />
        </el-form-item>
        <el-form-item label="代理人">
          <el-input v-model="newLeave.agentUserId" placeholder="工号" />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="newLeave.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker
            v-model="newLeave.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="产品型号">
          <el-select v-model="newLeave.productModels" multiple filterable allow-create default-first-option>
            <el-option label="RG-S6200-48XT8CQ" value="RG-S6200-48XT8CQ" />
            <el-option label="RG-RSR30-XA" value="RG-RSR30-XA" />
            <el-option label="RG-AP880" value="RG-AP880" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="saveLeave">保存请假代理配置</el-button>
      </el-form>

      <el-divider />

      <el-table :data="leaveList" border>
        <el-table-column prop="userId" label="请假人" />
        <el-table-column prop="agentUserId" label="代理人" />
        <el-table-column prop="startAt" label="开始时间" />
        <el-table-column prop="endAt" label="结束时间" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-title">消息推送开关</div>
      </template>
      <el-form label-width="180px">
        <el-form-item label="任务创建推送">
          <el-switch v-model="messageSwitch.taskCreated" />
        </el-form-item>
        <el-form-item label="任务转派推送">
          <el-switch v-model="messageSwitch.taskTransfer" />
        </el-form-item>
        <el-form-item label="任务超时推送">
          <el-switch v-model="messageSwitch.taskOverdue" />
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="saveSwitch">保存消息开关</el-button>
    </el-card>
  </div>
</template>

<style scoped>
.config-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.panel-card {
  border-radius: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
}
</style>
