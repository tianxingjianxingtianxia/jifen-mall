<template>
  <div class="product-detail-page">
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

    <div class="page-container">
      <div v-if="loading" class="loading-wrapper">
        <el-skeleton :rows="6" animated />
      </div>

      <template v-else-if="product">
        <el-card shadow="never">
          <el-row :gutter="40">
            <!-- 商品图片 -->
            <el-col :xs="24" :sm="12">
              <div class="detail-image-wrapper">
                <el-carousel
                  v-if="product.images && product.images.length > 0"
                  height="400px"
                  indicator-position="outside"
                >
                  <el-carousel-item v-for="(img, idx) in product.images" :key="idx">
                    <el-image :src="img" fit="contain" class="detail-image" />
                  </el-carousel-item>
                </el-carousel>
                <div v-else class="detail-image-single">
                  <el-image
                    :src="product.coverImage"
                    fit="contain"
                    class="detail-image"
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon :size="48"><Picture /></el-icon>
                        <p>暂无图片</p>
                      </div>
                    </template>
                  </el-image>
                </div>
              </div>
            </el-col>

            <!-- 商品信息 -->
            <el-col :xs="24" :sm="12">
              <div class="detail-info">
                <h2 class="detail-name">{{ product.name }}</h2>

                <div class="detail-points-row">
                  <span class="detail-points-label">所需积分</span>
                  <span class="detail-points-value">
                    <el-icon><Coin /></el-icon>
                    {{ product.pointsRequired }}
                  </span>
                </div>

                <div class="detail-meta">
                  <el-tag v-if="product.stockStatus === 'out_of_stock'" type="danger">
                    已售罄
                  </el-tag>
                  <el-tag v-else-if="product.stockStatus === 'low_stock'" type="warning">
                    库存紧张 ({{ product.stock }})
                  </el-tag>
                  <el-tag v-else type="success">有货</el-tag>

                  <span class="detail-sales">已售 {{ product.saleCount }} 件</span>
                </div>

                <el-divider />

                <div class="detail-description">
                  <h4 class="section-title">商品描述</h4>
                  <p class="description-text">
                    {{ product.description || '暂无描述' }}
                  </p>
                </div>

                <el-divider />

                <div class="detail-address">
                  <h4 class="section-title">收货地址</h4>
                  <el-select v-model="selectedAddressId" placeholder="选择收货地址" style="width: 100%">
                    <el-option
                      v-for="addr in addresses"
                      :key="addr.id"
                      :label="addr.receiverName + ' ' + addr.receiverPhone + ' ' + addr.province + addr.city + addr.district + addr.detailAddress"
                      :value="addr.id"
                    />
                  </el-select>
                  <el-button text type="primary" @click="router.push('/addresses')" style="margin-top: 8px">
                    + 新增地址
                  </el-button>
                </div>

                <el-divider />

                <div class="detail-action">
                  <el-button
                    type="warning"
                    size="large"
                    :disabled="product.stockStatus === 'out_of_stock' || exchangeLoading"
                    :loading="exchangeLoading"
                    :icon="ShoppingCart"
                    @click="handleExchange"
                  >
                    {{ product.stockStatus === 'out_of_stock' ? '已售罄' : '立即兑换' }}
                  </el-button>
                  <span class="my-points">
                    我的积分：{{ userStore.points }}
                  </span>
                </div>

                <div v-if="userStore.points < product.pointsRequired" class="insufficient-hint">
                  <el-alert
                    title="积分不足，无法兑换此商品"
                    type="warning"
                    :closable="false"
                    show-icon
                  />
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </template>

      <div v-else class="empty-wrapper">
        <el-empty description="商品不存在或已下架" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, User, ArrowDown, Coin, Picture, ShoppingCart
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getProductDetail, type ProductDetail } from '../api/products'
import { createOrder } from '../api/orders'
import { getAddresses, type Address } from '../api/addresses'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref<ProductDetail | null>(null)
const loading = ref(false)
const exchangeLoading = ref(false)
const addresses = ref<Address[]>([])
const selectedAddressId = ref<number | null>(null)
const showAddressDialog = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('商品ID无效')
    router.push('/home')
    return
  }
  await loadProduct(id)
  await loadAddresses()
})

async function loadAddresses() {
  try {
    addresses.value = await getAddresses()
    const defaultAddr = addresses.value.find(a => a.isDefault === 1 || a.isDefault === true)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id
    } else if (addresses.value.length > 0) {
      selectedAddressId.value = addresses.value[0].id
    }
  } catch {}
}

async function loadProduct(id: number) {
  loading.value = true
  try {
    product.value = await getProductDetail(id)
  } catch (e: any) {
    ElMessage.error(e.message || '加载商品详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

async function handleExchange() {
  if (!product.value) return
  if (userStore.points < product.value.pointsRequired) {
    ElMessage.warning('积分不足，无法兑换')
    return
  }
  if (addresses.value.length === 0) {
    ElMessage.warning('请先添加收货地址')
    router.push('/addresses')
    return
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认兑换 <b>' + product.value.name + '</b>？<br/>消耗积分：<b style="color:#e6a23c">' + product.value.pointsRequired + '</b>',
      '兑换确认',
      { confirmButtonText: '确认兑换', cancelButtonText: '取消', dangerouslyUseHTMLString: true, type: 'warning' }
    )
  } catch {
    return // 用户取消
  }
  exchangeLoading.value = true
  try {
    const order = await createOrder({
      productId: product.value.id,
      addressId: selectedAddressId.value
    })
    ElMessage.success('兑换成功！')
    userStore.updatePoints(userStore.points - product.value.pointsRequired)
    router.push('/order/' + order.id)
  } catch (e: any) {
    ElMessage.error(e.message || '兑换失败')
  } finally {
    exchangeLoading.value = false
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
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

/* 详情 */
.detail-image-wrapper {
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.detail-image,
.detail-image-single {
  width: 100%;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.image-placeholder {
  text-align: center;
  color: #c0c4cc;
}

.image-placeholder p {
  margin-top: 8px;
  font-size: 14px;
}

.detail-info {
  padding: 0 0 20px;
}

.detail-name {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.detail-points-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 8px;
}

.detail-points-label {
  font-size: 14px;
  color: #909399;
}

.detail-points-value {
  font-size: 28px;
  font-weight: 700;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.detail-sales {
  font-size: 14px;
  color: #909399;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.description-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.detail-action {
  display: flex;
  align-items: center;
  gap: 16px;
}

.my-points {
  font-size: 14px;
  color: #909399;
}

.insufficient-hint {
  margin-top: 12px;
}

.loading-wrapper {
  padding: 40px;
}

.empty-wrapper {
  padding: 80px 0;
}
</style>
