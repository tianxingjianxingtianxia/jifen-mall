<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, ArrowDown } from '@element-plus/icons-vue'
import { getOrders, cancelOrder, confirmReceipt, STATUS_MAP } from '../api/orders'
import type { OrderVO } from '../api/orders'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()

const orders = ref<OrderVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeStatus = ref<string>('')
const loading = ref(false)

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待发货', value: '0' },
  { label: '已发货', value: '1' },
  { label: '已完成', value: '2' },
  { label: '已取消', value: '3' },
]

const fetchOrders = async () => {
  loading.value = true
  try {
    const status = activeStatus.value === '' ? undefined : Number(activeStatus.value)
    const res = await getOrders({
      status: status,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    orders.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const onTabChange = (tabName: string) => {
  activeStatus.value = tabName
  currentPage.value = 1
  fetchOrders()
}

const handleCancel = async (order: OrderVO) => {
  try {
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (e: any) {
    ElMessage.error(e.message || '取消失败')
  }
}

const handleConfirm = async (order: OrderVO) => {
  try {
    await confirmReceipt(order.id)
    ElMessage.success('已确认收货')
    fetchOrders()
  } catch (e: any) {
    ElMessage.error(e.message || '确认收货失败')
  }
}

const goToDetail = (id: number) => {
  router.push(`/order/${id}`)
}

const canCancel = (order: OrderVO) => order.status === 0
const canConfirm = (order: OrderVO) => order.status === 1

function goBack() {
  router.back()
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(fetchOrders)
</script>

<template>
  <div class="orders-page">
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

    <el-tabs :model-value="activeStatus" @tab-change="onTabChange">
      <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div v-loading="loading" class="order-list">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />

      <div v-for="order in orders" :key="order.id" class="order-card" @click="goToDetail(order.id)">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="order.status === 0 ? 'warning' : order.status === 1 ? 'primary' : order.status === 2 ? 'success' : 'info'">
            {{ order.statusText }}
          </el-tag>
        </div>

        <div class="order-body">
          <el-image class="product-img" :src="order.productImage || '/placeholder.png'" fit="cover" />
          <div class="order-info">
            <div class="product-name">{{ order.productName }}</div>
            <div class="points">消耗 <el-tag type="danger">{{ order.pointsSpent }}</el-tag> 积分</div>
            <div class="receiver" v-if="order.receiverName">收货人：{{ order.receiverName }} {{ order.receiverPhone }}</div>
          </div>
        </div>

        <div class="order-footer" @click.stop>
          <span class="order-time">{{ order.createTime }}</span>
          <div class="actions">
            <el-button v-if="canCancel(order)" size="small" type="danger" plain @click="handleCancel(order)">取消订单</el-button>
            <el-button v-if="canConfirm(order)" size="small" type="primary" @click="handleConfirm(order)">确认收货</el-button>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchOrders"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
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
.order-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.order-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.order-no {
  font-size: 12px;
  color: #909399;
}
.order-body {
  display: flex;
  gap: 16px;
}
.product-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  background: #f5f7fa;
}
.order-info {
  flex: 1;
}
.product-name {
  font-weight: 600;
  margin-bottom: 4px;
}
.points {
  margin: 4px 0;
  font-size: 14px;
}
.receiver {
  font-size: 12px;
  color: #909399;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.order-time {
  font-size: 12px;
  color: #c0c4cc;
}
.actions {
  display: flex;
  gap: 8px;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
