<template>
  <div class="product-detail-page">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h1 class="header-title">商品详情</h1>
        <div class="header-right">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.nickname }}
          </span>
          <el-button text type="danger" @click="handleLogout">退出</el-button>
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

                <div class="detail-action">
                  <el-button
                    type="warning"
                    size="large"
                    :disabled="product.stockStatus === 'out_of_stock'"
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, User, Coin, Picture, ShoppingCart
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getProductDetail, type ProductDetail } from '../api/products'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref<ProductDetail | null>(null)
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('商品ID无效')
    router.push('/home')
    return
  }
  await loadProduct(id)
})

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

function handleExchange() {
  if (!product.value) return
  if (userStore.points < product.value.pointsRequired) {
    ElMessage.warning('积分不足，无法兑换')
    return
  }
  ElMessage.info('兑换功能开发中，敬请期待')
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-title {
  flex: 1;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
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
