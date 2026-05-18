<template>
  <div class="config-page">
    <h3 class="page-title">系统配置</h3>

    <el-card shadow="never">
      <el-form ref="formRef" :model="config" label-width="160px" v-loading="loading">
        <el-form-item
          v-for="(value, key) in config"
          :key="key"
          :label="configLabel(key)"
          :prop="key"
          :rules="[{ required: true, message: '请输入配置值', trigger: 'blur' }]"
        >
          <el-input
            v-if="!isTextarea(key)"
            v-model="config[key]"
            :placeholder="'请输入' + configLabel(key)"
          />
          <el-input
            v-else
            v-model="config[key]"
            type="textarea"
            :rows="3"
            :placeholder="'请输入' + configLabel(key)"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
          <el-button @click="loadConfig">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { getAdminConfig, updateAdminConfig } from '../../api/admin'

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

const config = reactive<Record<string, string>>({})

const labelMap: Record<string, string> = {
  sign_in_points: '每日签到奖励积分',
  exchange_ratio: '积分兑换比例（多少积分兑换1元）',
  order_expire_minutes: '未付款订单自动取消时间（分钟）',
  repeat_exchange_days: '商品重复兑换间隔（天）',
  signInPoints: '每日签到奖励积分',
  exchangeRate: '积分兑换比例（多少积分兑换1元）',
  dailySignInLimit: '每日最多签到次数',
  orderTimeoutMinutes: '未付款订单自动取消时间（分钟）',
  maxProductsPerPage: '商品列表每页显示数量',
  pointsValidityDays: '积分有效期天数（过期后积分不可用）',
  systemNotice: '系统公告（显示在首页顶部）',
  contactPhone: '客服联系电话',
  contactEmail: '客服邮箱'
}

const textareaKeys: string[] = ['systemNotice']

function configLabel(key: string): string {
  return labelMap[key] || key
}

function isTextarea(key: string): boolean {
  return textareaKeys.includes(key)
}

onMounted(() => {
  loadConfig()
})

async function loadConfig() {
  loading.value = true
  try {
    const data = await getAdminConfig()
    // 清空并填充 config
    Object.keys(config).forEach(k => delete config[k])
    for (const [key, value] of Object.entries(data)) {
      config[key] = String(value)
    }
  } catch (e: any) {
    ElMessage.error('获取配置失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateAdminConfig(config)
    ElMessage.success('配置保存成功')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.config-page {
  padding: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}
</style>
