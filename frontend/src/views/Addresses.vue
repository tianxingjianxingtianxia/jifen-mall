<template>
  <div class="addresses-page">
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
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            新增地址
          </el-button>
        </div>
      </div>
    </header>

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
import { ArrowLeft, Plus, User, ArrowDown } from '@element-plus/icons-vue'
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
