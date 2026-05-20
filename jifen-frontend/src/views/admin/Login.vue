<template>
  <div class="admin-login-page">
    <div class="admin-login-card">
      <h2 class="admin-login-title">积分商城 - 管理后台</h2>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="管理员用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="admin-login-btn"
            @click="handleLogin"
          >
            管理员登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="admin-login-footer">
        <router-link to="/login">用户登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { adminLogin } from '../../api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入管理员用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await adminLogin(form)
    localStorage.setItem('token', result.token)
    localStorage.setItem('isAdmin', 'true')
    if (result.nickname) {
      localStorage.setItem('adminName', result.nickname)
    }
    ElMessage.success('管理员登录成功')
    router.push('/admin/dashboard')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.admin-login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.admin-login-title {
  text-align: center;
  font-size: 24px;
  color: #303133;
  margin-bottom: 30px;
  font-weight: 600;
}

.admin-login-btn {
  width: 100%;
}

.admin-login-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.admin-login-footer a {
  color: #409eff;
  text-decoration: none;
}
</style>
