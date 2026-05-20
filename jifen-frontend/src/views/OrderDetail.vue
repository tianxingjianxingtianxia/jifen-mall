<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '../api/orders'
import type { OrderVO } from '../api/orders'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const order = ref<OrderVO | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    order.value = await getOrderDetail(id)
  } catch {
    ElMessage.error('订单不存在')
    router.push('/orders')
  } finally {
    loading.value = false
  }
})

const statusTagType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return map[status] || 'info'
}
</script>

<template>
  <div class="order-detail" v-loading="loading">
    <div class="back-link">
      <el-button text @click="router.push('/orders')">← 返回订单列表</el-button>
    </div>

    <div v-if="order" class="detail-card">
      <div class="status-bar">
        <el-tag :type="statusTagType(order.status)" size="large">{{ order.statusText }}</el-tag>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ order.productName }}</el-descriptions-item>
        <el-descriptions-item label="消耗积分">{{ order.pointsSpent }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ order.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="物流单号">{{ order.trackingNo || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="order.paidAt" label="兑换时间">{{ order.paidAt }}</el-descriptions-item>
        <el-descriptions-item v-if="order.shippedAt" label="发货时间">{{ order.shippedAt }}</el-descriptions-item>
        <el-descriptions-item v-if="order.confirmedAt" label="完成时间">{{ order.confirmedAt }}</el-descriptions-item>
        <el-descriptions-item v-if="order.cancelReason" label="取消原因">{{ order.cancelReason }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<style scoped>
.order-detail {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}
.back-link {
  margin-bottom: 16px;
}
.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}
.status-bar {
  margin-bottom: 20px;
}
</style>
