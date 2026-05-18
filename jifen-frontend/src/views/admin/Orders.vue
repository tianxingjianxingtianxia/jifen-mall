<template>
  <div class="orders-page">
    <div class="page-header">
      <h3 class="page-title">订单管理</h3>
    </div>

    <!-- 状态筛选 Tabs -->
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待发货" name="1" />
        <el-tab-pane label="已发货" name="2" />
        <el-tab-pane label="已完成" name="3" />
        <el-tab-pane label="已取消" name="4" />
      </el-tabs>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="搜索订单号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="orders" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column prop="pointsSpent" label="消耗积分" width="100" />
        <el-table-column prop="receiverName" label="收货人" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="trackingNo" label="物流单号" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              size="small"
              type="primary"
              @click="openShipDialog(row)"
            >
              发货
            </el-button>
            <span v-else class="no-action">--</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="loadOrders"
        />
      </div>
    </el-card>

    <!-- 发货弹窗 -->
    <el-dialog
      v-model="shipDialogVisible"
      title="填写物流单号"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="90px">
        <el-form-item label="物流单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getAdminOrders,
  shipOrder,
  type OrderItem
} from '../../api/admin'

const orders = ref<OrderItem[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const activeTab = ref('all')

const searchForm = reactive({
  orderNo: ''
})

// 发货相关
const shipDialogVisible = ref(false)
const shipping = ref(false)
const currentOrderId = ref<number | null>(null)
const shipFormRef = ref<FormInstance>()
const shipForm = reactive({
  trackingNo: ''
})
const shipRules: FormRules = {
  trackingNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }]
}

function statusType(status: number): string {
  switch (status) {
    case 1: return 'warning'
    case 2: return 'primary'
    case 3: return 'success'
    case 4: return 'info'
    default: return 'info'
  }
}

onMounted(() => {
  loadOrders()
})

async function loadOrders() {
  loading.value = true
  try {
    const status = activeTab.value === 'all' ? undefined : Number(activeTab.value)
    const res: any = await getAdminOrders({
      status,
      orderNo: searchForm.orderNo || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.records) {
      orders.value = res.records
      total.value = res.total
    } else if (Array.isArray(res)) {
      orders.value = res
      total.value = res.length
    }
  } catch (e: any) {
    ElMessage.error('获取订单列表失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  pageNum.value = 1
  loadOrders()
}

function search() {
  pageNum.value = 1
  loadOrders()
}

function resetSearch() {
  searchForm.orderNo = ''
  search()
}

function openShipDialog(row: OrderItem) {
  currentOrderId.value = row.id
  shipForm.trackingNo = ''
  shipDialogVisible.value = true
}

async function handleShip() {
  const valid = await shipFormRef.value?.validate().catch(() => false)
  if (!valid || currentOrderId.value === null) return

  shipping.value = true
  try {
    await shipOrder(currentOrderId.value, shipForm.trackingNo)
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    loadOrders()
  } catch (e: any) {
    ElMessage.error(e.message || '发货失败')
  } finally {
    shipping.value = false
  }
}
</script>

<style scoped>
.orders-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.search-form {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.no-action {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
