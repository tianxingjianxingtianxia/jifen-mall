<template>
  <div class="products-page">
    <div class="page-header">
      <h3 class="page-title">商品管理</h3>
      <el-button type="primary" @click="openCreateDialog">新增商品</el-button>
    </div>

    <!-- 搜索 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.keyword" placeholder="搜索商品名称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="products" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImage"
              :src="row.coverImage"
              style="width: 50px; height: 50px; border-radius: 4px;"
              fit="cover"
            >
              <template #error>
                <div class="image-slot" style="width: 50px; height: 50px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 4px;">
                  <el-icon><PictureFilled /></el-icon>
                </div>
              </template>
            </el-image>
            <span v-else style="color: #c0c4cc;">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="pointsRequired" label="所需积分" width="100" />
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="saleCount" label="销量" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="loadProducts"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
        </el-form-item>
        <el-form-item label="封面图URL" prop="coverImage">
          <el-input v-model="form.coverImage" placeholder="请输入封面图片URL" />
          <div v-if="form.coverImage" style="margin-top: 8px;">
            <el-image
              :src="form.coverImage"
              style="width: 120px; height: 120px; border-radius: 4px; border: 1px solid #dcdfe6;"
              fit="cover"
              @error="onImageError"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><PictureFilled /></el-icon>
                  <span style="font-size: 12px; color: #909399;">加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
        </el-form-item>
        <el-form-item label="所需积分" prop="pointsRequired">
          <el-input-number v-model="form.pointsRequired" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { PictureFilled } from '@element-plus/icons-vue'
import {
  getAdminProducts,
  createAdminProduct,
  updateAdminProduct,
  toggleProductStatus,
  deleteAdminProduct,
  type ProductItem
} from '../../api/admin'

const products = ref<ProductItem[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  keyword: ''
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  description: '',
  coverImage: '',
  pointsRequired: 0,
  stock: 0,
  sortOrder: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  pointsRequired: [{ required: true, message: '请设置所需积分', trigger: 'blur' }],
  stock: [{ required: true, message: '请设置库存', trigger: 'blur' }]
}

function onImageError(e: Event) {
  console.warn('图片加载失败:', (e.target as HTMLImageElement).src)
}

onMounted(() => {
  loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    const res: any = await getAdminProducts({
      keyword: searchForm.keyword || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    // 支持分页和非分页返回格式
    if (res.records) {
      products.value = res.records
      total.value = res.total
    } else if (Array.isArray(res)) {
      products.value = res
      total.value = res.length
    }
  } catch (e: any) {
    ElMessage.error('获取商品列表失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  loadProducts()
}

function resetSearch() {
  searchForm.keyword = ''
  search()
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.coverImage = ''
  form.pointsRequired = 0
  form.stock = 0
  form.sortOrder = 0
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: ProductItem) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.description = row.description
  form.coverImage = row.coverImage
  form.pointsRequired = row.pointsRequired
  form.stock = row.stock
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

async function saveProduct() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value && editId.value !== null) {
      await updateAdminProduct(editId.value, form)
      ElMessage.success('商品更新成功')
    } else {
      await createAdminProduct(form)
      ElMessage.success('商品创建成功')
    }
    dialogVisible.value = false
    loadProducts()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: ProductItem) {
  try {
    await toggleProductStatus(row.id)
    ElMessage.success(row.status === 1 ? '已下架' : '已上架')
    loadProducts()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row: ProductItem) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAdminProduct(row.id)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.products-page {
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

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
