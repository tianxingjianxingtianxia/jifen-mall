<template>
  <div class="points-records-page">
    <!-- 顶部导航 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2>积分明细</h2>
      </div>
      <div class="top-bar-right">
        <span class="user-info">
          <el-icon><User /></el-icon>
          {{ userStore.nickname || userStore.userInfo?.username }}
        </span>
        <el-button text type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div v-loading="loading" class="table-wrapper">
      <el-table
        :data="records"
        stripe
        style="width: 100%"
        empty-text="暂无积分记录"
      >
        <el-table-column label="时间" prop="createTime" width="180" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.type === 1" type="success" size="small">获得</el-tag>
            <el-tag v-else type="danger" size="small">消耗</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动积分" width="120">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'points-earned' : 'points-spent'">
              {{ row.type === 1 ? '+' : '-' }}{{ row.points }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="来源" prop="source" width="160" />
        <el-table-column label="备注" prop="remark" min-width="200" show-overflow-tooltip />
      </el-table>

      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchRecords"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User } from '@element-plus/icons-vue'
import { getPointsRecords } from '@/api/points'
import type { PointsRecord } from '@/api/points'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const records = ref<PointsRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

async function fetchRecords() {
  loading.value = true
  try {
    const res = await getPointsRecords({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    records.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载积分记录失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(fetchRecords)
</script>

<style scoped>
.points-records-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-bar-left h2 {
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
}

.table-wrapper {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
}

.points-earned {
  color: #67c23a;
  font-weight: 600;
}

.points-spent {
  color: #f56c6c;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
