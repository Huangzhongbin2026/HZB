<template>
  <div class="forbidden-wrap">
    <h1>403</h1>
    <p>当前账号无权访问该页面，请切换角色视角或联系管理员。</p>
    <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<style scoped>
.forbidden-wrap {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}

h1 {
  margin: 0;
  font-size: 72px;
  color: #e6a23c;
}

p {
  margin: 0;
  color: #606266;
}
</style>
