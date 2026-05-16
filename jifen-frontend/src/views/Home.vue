<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-inner">
        <h1 class="logo">积分商城</h1>
        <div class="header-right">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.nickname || userStore.userInfo?.username }}
          </span>
          <el-button text type="primary" @click="router.push('/addresses')">
            <el-icon><Location /></el-icon> 地址管理
          </el-button>
          <el-button text type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
    </header>

    <div class="page-container">
      <!-- 积分卡片 -->
      <el-row :gutter="20" class="points-section">
        <el-col :span="8">
          <el-card shadow="hover" class="points-card">
            <div class="points-card-body">
              <div class="points-icon">
                <el-icon :size="40" color="#e6a23c"><Coin /></el-icon>
              </div>
              <div class="points-info">
                <div class="points-label">当前积分</div>
                <div class="points-value">{{ userStore.points }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="points-card">
            <div class="points-card-body">
              <div class="points-icon">
                <el-icon :size="40" color="#67c23a"><Check /></el-icon>
              </div>
              <div class="points-info">
                <div class="points-label">今日签到</div>
                <el-button
                  :type="todaySigned ? 'success' : 'primary'"
                  :disabled="todaySigned"
                  :loading="signing"
                  @click="handleSignIn"
                >
                  {{ todaySigned ? '已签到' : '签到' }}
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="points-card">
            <div class="points-card-body">
              <div class="points-icon">
                <el-icon :size="40" color="#409eff"><TrendCharts /></el-icon>
              </div>
              <div class="points-info">
                <div class="points-label">积分记录</div>
                <div class="points-sub" v-if="balance">
                  累计获得 {{ balance.totalEarned }} · 消费 {{ balance.totalSpent }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索和排序 -->
      <el-card shadow="never" class="search-section">
        <el-row :gutter="16" align="middle">
          <el-col :span="8">
            <el-input
              v-model="keyword"
              placeholder="搜索商品"
              :prefix-icon="Search"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </el-col>
          <el-col :span="6">
            <el-select v-model="sortBy" placeholder="排序方式" @change="handleSortChange">
              <el-option label="默认排序" value="" />
              <el-option label="积分从低到高" value="points_asc" />
              <el-option label="积分从高到低" value="points_desc" />
              <el-option label="销量从高到低" value="sale_desc" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
          </el-col>
        </el-row>
      </el-card>

      <!-- 商品网格 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="products.length === 0" class="empty-container">
        <el-empty description="暂无商品" />
      </div>

      <el-row v-else :gutter="20" class="product-grid">
        <el-col
          v-for="product in products"
          :key="product.id"
          :xs="12"
          :sm="8"
          :md="6"
          :lg="6"
          class="product-item"
        >
          <el-card
            shadow="hover"
            :body-style="{ padding: '0' }"
            class="product-card"
            @click="goToProduct(product.id)"
          >
            <div class="product-image-wrapper">
              <el-image
                :src="product.coverImage"
                fit="cover"
                class="product-image"
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon :size="32"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <el-tag
                v-if="product.stockStatus === 'out_of_stock'"
                class="stock-tag"
                type="danger"
                size="small"
              >
                已售罄
              </el-tag>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <div class="product-meta">
                <span class="product-points">
                  <el-icon><Coin /></el-icon>
                  {{ product.pointsRequired }}
                </span>
                <span class="product-sales">已售 {{ product.saleCount }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 36, 48]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Location, Coin, Check, TrendCharts,
  Search, Picture
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getTodaySign, signIn, getPointsBalance } from '../api/points'
import { getProducts, type ProductItem } from '../api/products'
import type { PointsBalance } from '../api/points'

const router = useRouter()
const userStore = useUserStore()

const products = ref<ProductItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const keyword = ref('')
const sortBy = ref('')
const loading = ref(false)

const todaySigned = ref(false)
const signing = ref(false)
const balance = ref<PointsBalance | null>(null)

onMounted(async () => {
  // 刷新用户信息
  await userStore.fetchUserInfo()
  // 加载数据
  await Promise.all([
    loadProducts(),
    checkTodaySign(),
    loadBalance()
  ])
})

async function checkTodaySign() {
  try {
    todaySigned.value = await getTodaySign()
  } catch {
    // ignore
  }
}

async function loadBalance() {
  try {
    balance.value = await getPointsBalance()
  } catch {
    // ignore
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const result = await getProducts({
      keyword: keyword.value || undefined,
      sortBy: sortBy.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    products.value = result.records
    total.value = result.total
  } catch (e: any) {
    ElMessage.error(e.message || '加载商品失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadProducts()
}

function handleSortChange() {
  pageNum.value = 1
  loadProducts()
}

function handlePageChange() {
  loadProducts()
}

async function handleSignIn() {
  signing.value = true
  try {
    const result = await signIn()
    todaySigned.value = result.todaySigned
    userStore.updatePoints(result.totalPoints)
    ElMessage.success(`签到成功！获得 ${result.points} 积分`)
    loadBalance()
  } catch (e: any) {
    ElMessage.error(e.message || '签到失败')
  } finally {
    signing.value = false
  }
}

function goToProduct(id: number) {
  router.push(`/product/${id}`)
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
  justify-content: space-between;
}

.logo {
  font-size: 22px;
  color: #303133;
  font-weight: 600;
}

.header-right {
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

/* 积分卡片 */
.points-section {
  margin-bottom: 20px;
}

.points-card {
  cursor: pointer;
}

.points-card-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.points-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 12px;
}

.points-info {
  flex: 1;
}

.points-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.points-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.points-sub {
  font-size: 12px;
  color: #909399;
}

/* 搜索区域 */
.search-section {
  margin-bottom: 20px;
}

/* 商品网格 */
.product-grid {
  margin-bottom: 20px;
}

.product-item {
  margin-bottom: 20px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f7fa;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.stock-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.product-info {
  padding: 12px 16px;
}

.product-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-points {
  font-size: 18px;
  font-weight: 700;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 2px;
}

.product-sales {
  font-size: 12px;
  color: #909399;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.loading-container {
  padding: 40px;
}

.empty-container {
  padding: 60px 0;
}
</style>
