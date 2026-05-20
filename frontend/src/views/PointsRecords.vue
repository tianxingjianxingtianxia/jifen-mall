<template>
  <div class="points-records-page">
    <header class="header">
      <div class="header-inner">
        <h1 class="logo">积分商城</h1>
        <div class="header-nav">
          <router-link to="/home" class="nav-link">首页</router-link>
          <router-link to="/orders" class="nav-link">我的订单</router-link>
          <router-link to="/points-records" class="nav-link">积分明细</router-link>
          <router-link to="/addresses" class="nav-link">地址管理</router-link>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.nickname || userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/points-records')">积分明细</el-dropdown-item>
                <el-dropdown-item @click="router.push('/addresses')">地址管理</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

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
import { ArrowLeft, User, ArrowDown } from '@element-plus/icons-vue'
import { getPointsRecords } from '../api/points'
import type { PointsRecord } from '../api/points'
import { useUserStore } from '../stores/user'

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

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.3);
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  font-size: 22px;
  color: #fff;
  font-weight: 700;
  letter-spacing: 1px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-right .user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: rgba(255,255,255,0.9);
  font-size: 14px;
}
.header-right .user-info:hover {
  color: #fff;
}
.header-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}
.nav-link {
  color: rgba(255,255,255,0.75);
  text-decoration: none;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.2s;
}
.nav-link:hover {
  color: #fff;
  background: rgba(255,255,255,0.15);
}
.nav-link.router-link-active {
  color: #fff;
  font-weight: 600;
  background: rgba(255,255,255,0.2);
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
