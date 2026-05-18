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
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchUsers, toggleUserStatus } from '../../api/admin'

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
