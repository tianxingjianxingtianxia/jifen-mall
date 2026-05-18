<template>
  <div class="points-manage-page">
    <div class="page-header">
      <h3 class="page-title">积分管理</h3>
    </div>

    <!-- 搜索用户 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名/手机号">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索用户名、昵称或手机号"
            clearable
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 搜索结果 -->
    <el-card shadow="never" class="result-card">
      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="points" label="当前积分" width="100" />
        <el-table-column prop="totalEarned" label="累计获得" width="100" />
        <el-table-column prop="totalSpent" label="累计消耗" width="100" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openAdjustDialog(row)">
              调整积分
            </el-button>
            <el-button size="small" type="success" @click="openReferralDialog(row)">
              转介绍签约
            </el-button>
            <el-button size="small" type="warning" @click="openFollowupDialog(row)">
              售后回访
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="search"
        />
      </div>
    </el-card>

    <!-- 调整积分弹窗 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="调整积分"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="当前用户">
          <el-input :model-value="selectedUser?.username + '（积分：' + selectedUser?.points + '）'" disabled />
        </el-form-item>
        <el-form-item label="调整积分" prop="points">
          <el-input-number v-model="adjustForm.points" :min="-99999" :max="99999" />
          <div class="form-tip">正数增加，负数扣减</div>
        </el-form-item>
        <el-form-item label="积分来源" prop="source">
          <el-select v-model="adjustForm.source" placeholder="选择积分来源" style="width: 100%">
            <el-option label="签到" value="SIGN_IN" />
            <el-option label="兑换" value="EXCHANGE" />
            <el-option label="取消订单" value="ORDER_CANCEL" />
            <el-option label="转介绍签约" value="REFERRAL" />
            <el-option label="售后回访" value="FOLLOWUP" />
            <el-option label="手动调整" value="MANUAL_ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustForm.remark" type="textarea" :rows="2" placeholder="调整备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 转介绍签约弹窗 -->
    <el-dialog
      v-model="referralDialogVisible"
      title="转介绍签约 - 奖励积分"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form ref="referralFormRef" :model="referralForm" :rules="referralRules" label-width="100px">
        <el-form-item label="当前用户">
          <el-input :model-value="selectedUser?.username" disabled />
        </el-form-item>
        <el-form-item label="奖励积分" prop="points">
          <el-input-number v-model="referralForm.points" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="referralForm.remark" type="textarea" :rows="2" placeholder="转介绍备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="referralDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleReferral">确认奖励</el-button>
      </template>
    </el-dialog>

    <!-- 售后回访弹窗 -->
    <el-dialog
      v-model="followupDialogVisible"
      title="售后回访 - 奖励积分"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form ref="followupFormRef" :model="followupForm" :rules="followupRules" label-width="100px">
        <el-form-item label="当前用户">
          <el-input :model-value="selectedUser?.username" disabled />
        </el-form-item>
        <el-form-item label="奖励积分" prop="points">
          <el-input-number v-model="followupForm.points" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="followupForm.remark" type="textarea" :rows="2" placeholder="回访备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleFollowup">确认奖励</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { searchUsers, adjustUserPoints } from '../../api/admin'

interface UserItem {
  id: number
  username: string
  nickname: string
  phone: string
  points: number
  totalEarned: number
  totalSpent: number
}

const users = ref<UserItem[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  keyword: ''
})

// 调整积分弹窗
const adjustDialogVisible = ref(false)
const adjustFormRef = ref<FormInstance>()
const adjustForm = reactive({
  points: 10,
  source: 'MANUAL_ADJUST',
  remark: ''
})
const adjustRules: FormRules = {
  points: [{ required: true, message: '请输入调整积分', trigger: 'blur' }],
  source: [{ required: true, message: '请选择积分来源', trigger: 'change' }]
}

// 转介绍签约弹窗
const referralDialogVisible = ref(false)
const referralFormRef = ref<FormInstance>()
const referralForm = reactive({
  points: 50,
  remark: ''
})
const referralRules: FormRules = {
  points: [{ required: true, message: '请输入奖励积分', trigger: 'blur' }]
}

// 售后回访弹窗
const followupDialogVisible = ref(false)
const followupFormRef = ref<FormInstance>()
const followupForm = reactive({
  points: 30,
  remark: ''
})
const followupRules: FormRules = {
  points: [{ required: true, message: '请输入奖励积分', trigger: 'blur' }]
}

const selectedUser = ref<UserItem | null>(null)
const saving = ref(false)

onMounted(() => {
  search()
})

async function search() {
  loading.value = true
  try {
    const res: any = await searchUsers({
      keyword: searchForm.keyword || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.records) {
      users.value = res.records
      total.value = res.total
    } else if (Array.isArray(res)) {
      users.value = res
      total.value = res.length
    }
  } catch (e: any) {
    ElMessage.error('搜索用户失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.keyword = ''
  pageNum.value = 1
  search()
}

function openAdjustDialog(row: UserItem) {
  selectedUser.value = row
  adjustForm.points = 10
  adjustForm.source = 'MANUAL_ADJUST'
  adjustForm.remark = ''
  adjustDialogVisible.value = true
}

async function handleAdjust() {
  const valid = await adjustFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedUser.value) return

  saving.value = true
  try {
    await adjustUserPoints(selectedUser.value.id, adjustForm.points, adjustForm.source, adjustForm.remark)
    ElMessage.success('积分调整成功')
    adjustDialogVisible.value = false
    search()
  } catch (e: any) {
    ElMessage.error(e.message || '调整失败')
  } finally {
    saving.value = false
  }
}

function openReferralDialog(row: UserItem) {
  selectedUser.value = row
  referralForm.points = 50
  referralForm.remark = ''
  referralDialogVisible.value = true
}

async function handleReferral() {
  const valid = await referralFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedUser.value) return

  saving.value = true
  try {
    await adjustUserPoints(selectedUser.value.id, referralForm.points, 'REFERRAL', referralForm.remark || '转介绍签约奖励')
    ElMessage.success('转介绍签约奖励积分成功')
    referralDialogVisible.value = false
    search()
  } catch (e: any) {
    ElMessage.error(e.message || '奖励失败')
  } finally {
    saving.value = false
  }
}

function openFollowupDialog(row: UserItem) {
  selectedUser.value = row
  followupForm.points = 30
  followupForm.remark = ''
  followupDialogVisible.value = true
}

async function handleFollowup() {
  const valid = await followupFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedUser.value) return

  saving.value = true
  try {
    await adjustUserPoints(selectedUser.value.id, followupForm.points, 'FOLLOWUP', followupForm.remark || '售后回访奖励')
    ElMessage.success('售后回访奖励积分成功')
    followupDialogVisible.value = false
    search()
  } catch (e: any) {
    ElMessage.error(e.message || '奖励失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.points-manage-page {
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

.result-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
