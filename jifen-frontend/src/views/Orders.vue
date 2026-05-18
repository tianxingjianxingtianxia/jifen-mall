<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User } from '@element-plus/icons-vue'
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
    <div class="top-bar">
      <div class="top-bar-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2>我的订单</h2>
      </div>
      <div class="top-bar-right">
        <span class="user-info">
          <el-icon><User /></el-icon>
          {{ userStore.nickname || userStore.userInfo?.username }}
        </span>
        <el-button text type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

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
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 12px 24px;
  margin: -20px -20px 20px;
}
.top-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.top-bar-left h2 {
  margin: 0;
  font-size: 18px;
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
