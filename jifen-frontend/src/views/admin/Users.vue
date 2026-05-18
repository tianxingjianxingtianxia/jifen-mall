<template>
  <div class="users-page">
    <div class="page-header">
      <h3 class="page-title">用户管理</h3>
    </div>

    <!-- 搜索 -->
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

    <!-- 用户列表 -->
    <el-card shadow="never" class="result-card">
      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="points" label="积分" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              @click="openAdjustDialog(row)"
            >
              调整积分
            </el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
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

    <!-- 调整积分对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="调整积分"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form label-width="90px">
        <el-form-item label="当前积分">
          <span style="font-size: 18px; font-weight: 600; color: #409eff">{{ currentPoints }}</span>
        </el-form-item>
        <el-form-item label="增减积分" required>
          <el-input-number
            v-model="adjustForm.points"
            :min="-999999"
            :max="999999"
            placeholder="正数增加，负数扣减"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 4px">正数增加积分，负数扣减积分</div>
        </el-form-item>
        <el-form-item label="来源" required>
          <el-select v-model="adjustForm.source" style="width: 100%">
            <el-option label="转介绍签约" value="转介绍签约" />
            <el-option label="售后回访" value="售后回访" />
            <el-option label="手动调整" value="手动调整" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="adjustForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可选填写备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="handleAdjustPoints">
          确认调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchUsers, toggleUserStatus, adjustUserPoints } from '../../api/admin'

interface UserItem {
  id: number
  username: string
  nickname: string
  phone: string
  points: number
  status: number
  createTime: string
}

const users = ref<UserItem[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  keyword: ''
})

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

async function handleToggleStatus(row: UserItem) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await toggleUserStatus(row.id)
    ElMessage.success(`${action}成功`)
    search()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || `${action}失败`)
    }
  }
}

// ===== 调整积分对话框 =====
const adjustDialogVisible = ref(false)
const adjustUserId = ref<number | null>(null)
const currentPoints = ref(0)
const adjustForm = reactive({
  points: 0,
  source: '手动调整',
  remark: ''
})
const adjusting = ref(false)

function openAdjustDialog(row: UserItem) {
  adjustUserId.value = row.id
  currentPoints.value = row.points
  adjustForm.points = 0
  adjustForm.source = '手动调整'
  adjustForm.remark = ''
  adjustDialogVisible.value = true
}

async function handleAdjustPoints() {
  if (adjustUserId.value === null) return
  if (adjustForm.points === 0) {
    ElMessage.warning('请输入增减积分数值')
    return
  }
  adjusting.value = true
  try {
    await adjustUserPoints(
      adjustUserId.value,
      adjustForm.points,
      adjustForm.source,
      adjustForm.remark || undefined
    )
    ElMessage.success('积分调整成功')
    adjustDialogVisible.value = false
    search()
  } catch (e: any) {
    ElMessage.error('积分调整失败：' + (e.message || '未知错误'))
  } finally {
    adjusting.value = false
  }
}
</script>

<style scoped>
.users-page {
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
</style>
