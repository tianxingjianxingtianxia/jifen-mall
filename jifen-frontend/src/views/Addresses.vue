<template>
  <div class="addresses-page">
    <!-- 顶部导航 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2>地址管理</h2>
      </div>
      <div class="top-bar-right">
        <span class="user-info">
          <el-icon><User /></el-icon>
          {{ userStore.nickname || userStore.userInfo?.username }}
        </span>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">
          新增地址
        </el-button>
        <el-button text type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <div class="page-container">
      <!-- 地址列表 -->
      <div v-if="loading" class="loading-wrapper">
        <el-skeleton :rows="4" animated />
      </div>

      <template v-else-if="addresses.length === 0">
        <el-empty description="暂无收货地址，请添加">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            添加地址
          </el-button>
        </el-empty>
      </template>

      <div v-else class="address-list">
        <el-card
          v-for="addr in addresses"
          :key="addr.id"
          shadow="hover"
          class="address-card"
        >
          <div class="address-card-body">
            <div class="address-info">
              <div class="address-name-phone">
                <span class="address-name">{{ addr.receiverName }}</span>
                <span class="address-phone">{{ addr.receiverPhone }}</span>
                <el-tag v-if="addr.isDefault" type="danger" size="small">默认</el-tag>
              </div>
              <div class="address-detail">
                {{ addr.province }}{{ addr.city }}{{ addr.district }}
                {{ addr.detailAddress }}
              </div>
            </div>
            <div class="address-actions">
              <el-button
                v-if="!addr.isDefault"
                text
                type="primary"
                size="small"
                @click="handleSetDefault(addr.id)"
              >
                设为默认
              </el-button>
              <el-button
                text
                type="primary"
                size="small"
                @click="openEditDialog(addr)"
              >
                编辑
              </el-button>
              <el-popconfirm
                title="确定删除该地址吗？"
                @confirm="handleDelete(addr.id)"
              >
                <template #reference>
                  <el-button text type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 新增/编辑地址对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑地址' : '新增地址'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        size="default"
      >
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="省" prop="province">
              <el-input v-model="form.province" placeholder="省" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="市" prop="city">
              <el-input v-model="form.city" placeholder="市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区" prop="district">
              <el-input v-model="form.district" placeholder="区" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Plus, User } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import {
  getAddresses,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress,
  type Address,
  type AddressInput
} from '../api/addresses'

const router = useRouter()
const userStore = useUserStore()

const addresses = ref<Address[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const formRef = ref<FormInstance>()
const form = reactive<AddressInput>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const rules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

onMounted(() => {
  loadAddresses()
})

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await getAddresses()
  } catch (e: any) {
    ElMessage.error(e.message || '加载地址失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.receiverName = ''
  form.receiverPhone = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.detailAddress = ''
  form.isDefault = false
}

function openAddDialog() {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(addr: Address) {
  isEdit.value = true
  editingId.value = addr.id
  form.receiverName = addr.receiverName
  form.receiverPhone = addr.receiverPhone
  form.province = addr.province
  form.city = addr.city
  form.district = addr.district
  form.detailAddress = addr.detailAddress
  form.isDefault = addr.isDefault
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const submitData = { ...form, isDefault: form.isDefault ? 1 : 0 }
    if (isEdit.value && editingId.value !== null) {
      await updateAddress(editingId.value, submitData)
      ElMessage.success('地址更新成功')
    } else {
      await addAddress(submitData)
      ElMessage.success('地址添加成功')
    }
    dialogVisible.value = false
    await loadAddresses()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleSetDefault(id: number) {
  try {
    await setDefaultAddress(id)
    ElMessage.success('已设为默认地址')
    await loadAddresses()
  } catch (e: any) {
    ElMessage.error(e.message || '设置失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteAddress(id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
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
</script>

<style scoped>
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

/* 地址列表 */
.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.address-info {
  flex: 1;
}

.address-name-phone {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.address-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.address-phone {
  font-size: 14px;
  color: #606266;
}

.address-detail {
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.loading-wrapper {
  padding: 40px;
}
</style>
