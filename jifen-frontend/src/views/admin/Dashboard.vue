<template>
  <div class="dashboard-page">
    <h3 class="page-title">数据看板</h3>

    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ dashboard.totalUsers }}</div>
          <div class="stat-label">总用户数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ dashboard.totalProducts }}</div>
          <div class="stat-label">上架商品</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ dashboard.totalOrders }}</div>
          <div class="stat-label">总订单</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-warning">
          <div class="stat-value">{{ dashboard.pendingOrders }}</div>
          <div class="stat-label">待处理订单</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-points">
          <div class="stat-value">{{ dashboard.totalPointsEarned }}</div>
          <div class="stat-label">总获得积分</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-points-spent">
          <div class="stat-value">{{ dashboard.totalPointsSpent }}</div>
          <div class="stat-label">总消耗积分</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-signin">
          <div class="stat-value">{{ dashboard.todaySignIns }}</div>
          <div class="stat-label">今日签到</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminDashboard, type DashboardVO } from '../../api/admin'

const dashboard = ref<DashboardVO>({
  totalUsers: 0,
  totalProducts: 0,
  totalOrders: 0,
  todaySignIns: 0,
  pendingOrders: 0,
  totalPointsEarned: 0,
  totalPointsSpent: 0
})

onMounted(async () => {
  try {
    const data = await getAdminDashboard()
    dashboard.value = data
  } catch (e: any) {
    ElMessage.error('获取看板数据失败：' + (e.message || '未知错误'))
  }
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  border-radius: 8px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #409eff;
  padding: 20px 0 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  padding-bottom: 10px;
}

.stat-warning .stat-value {
  color: #e6a23c;
}

.stat-points .stat-value {
  color: #67c23a;
}

.stat-points-spent .stat-value {
  color: #f56c6c;
}

.stat-signin .stat-value {
  color: #409eff;
}
</style>
