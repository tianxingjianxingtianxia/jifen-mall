<template>
  <div class="products-page">
    <div class="page-header">
      <h3 class="page-title">商品管理</h3>
      <div class="page-header-actions">
        <el-button type="success" @click="exportProducts">导出 CSV</el-button>
        <el-button type="primary" @click="openCreateDialog">新增商品</el-button>
      </div>
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
        <el-form-item label="商品图片" prop="coverImage">
          <div class="upload-wrapper">
            <!-- 多图片上传区域 -->
            <el-upload
              ref="uploadRef"
              action="http://localhost:8080/api/admin/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              multiple
              :before-upload="beforeUpload"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
            >
              <template #trigger>
                <el-button type="primary" :loading="uploading">选择图片（可多选）</el-button>
              </template>
              <template #tip>
                <div class="el-upload__tip">支持 jpg/png/gif，不超过 5MB，可多选</div>
              </template>
            </el-upload>
            <!-- 已上传图片缩略图列表 -->
            <div v-if="form.images.length > 0" class="image-thumbnail-list">
              <div v-for="(img, idx) in form.images" :key="idx" class="image-thumbnail-item">
                <el-image
                  :src="img"
                  style="width: 90px; height: 90px; border-radius: 4px; border: 1px solid #dcdfe6;"
                  fit="cover"
                  :preview-src-list="form.images"
                  preview-teleported
                >
                  <template #error>
                    <div class="image-slot">
                      <el-icon><PictureFilled /></el-icon>
                    </div>
                  </template>
                </el-image>
                <el-button class="remove-image-btn" size="small" type="danger" circle @click="removeImage(idx)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
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
import { PictureFilled, Close } from '@element-plus/icons-vue'
import {
  createAdminProduct,
  deleteAdminProduct,
  exportProductsCsv,
  getAdminProducts,
  toggleProductStatus,
  updateAdminProduct,
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
const uploadRef = ref(null)
const uploading = ref(false)
const uploadHeaders = { Authorization: 'Bearer ' + localStorage.getItem('token') }

const beforeUpload = (file: File) => {
  const validTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg/png/gif 格式')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  uploading.value = true
  return true
}

const onUploadSuccess = (response: any, file: File, fileList: any) => {
  uploading.value = false
  let url = ''
  if (response && response.code === 200 && response.data) {
    url = response.data
  } else if (response && response.url) {
    url = response.url
  } else {
    ElMessage.error('上传响应格式异常')
    return
  }
  // 添加到图片列表
  form.images.push(url)
  // 第一张图作为封面图
  if (form.images.length === 1) {
    form.coverImage = url
  }
  ElMessage.success('图片上传成功')
}

const onUploadError = (err: any, file: File, fileList: any) => {
  uploading.value = false
  ElMessage.error('图片上传失败')
}

const form = reactive({
  name: '',
  description: '',
  coverImage: '',
  pointsRequired: 0,
  stock: 0,
  sortOrder: 0,
  images: [] as string[]
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  pointsRequired: [{ required: true, message: '请设置所需积分', trigger: 'blur' }],
  stock: [{ required: true, message: '请设置库存', trigger: 'blur' }]
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

function exportProducts() {
  const params: any = {}
  if (searchForm.keyword) {
    params.keyword = searchForm.keyword
  }
  exportProductsCsv(params)
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.coverImage = ''
  form.pointsRequired = 0
  form.stock = 0
  form.sortOrder = 0
  form.images = []
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
  form.images = row.images ? [...row.images] : []
  dialogVisible.value = true
}

function removeImage(idx: number) {
  form.images.splice(idx, 1)
  // 如果删除了封面图，更新封面图为第一张或清空
  if (form.images.length > 0) {
    form.coverImage = form.images[0]
  } else {
    form.coverImage = ''
  }
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

.page-header-actions {
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.upload-wrapper { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; }
.upload-preview { position: relative; display: inline-block; }
.remove-image-btn { position: absolute; top: -8px; right: -8px; }
.image-slot { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 90px; height: 90px; background: #f5f7fa; border-radius: 4px; }
.image-thumbnail-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px; }
.image-thumbnail-item { position: relative; display: inline-block; }
</style>
