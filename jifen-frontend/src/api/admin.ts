import request from '../utils/request'

export interface ProductItem {
  id: number
  name: string
  description: string
  coverImage: string
  pointsRequired: number
  stock: number
  status: number
  sortOrder: number
  saleCount: number
  createTime: string
  updateTime: string
  images?: string[]
}

export interface OrderItem {
  id: number
  orderNo: string
  productName: string
  pointsSpent: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  status: number
  statusText: string
  trackingNo: string
  createTime: string
}

export interface DashboardVO {
  totalUsers: number
  totalProducts: number
  totalOrders: number
  todaySignIns: number
  pendingOrders: number
  totalPointsEarned: number
  totalPointsSpent: number
}

// 商品管理
export const getAdminProducts = (params: { keyword?: string; status?: number; pageNum?: number; pageSize?: number }) =>
  request.get('/admin/products', { params })

export const createAdminProduct = (data: any) =>
  request.post('/admin/products', data)

export const updateAdminProduct = (id: number, data: any) =>
  request.put(`/admin/products/${id}`, data)

export const toggleProductStatus = (id: number) =>
  request.put(`/admin/products/${id}/status`)

export const deleteAdminProduct = (id: number) =>
  request.delete(`/admin/products/${id}`)

// 订单管理
export const getAdminOrders = (params: { status?: number; orderNo?: string; pageNum?: number; pageSize?: number }) =>
  request.get('/admin/orders', { params })

export const shipOrder = (id: number, trackingNo: string) =>
  request.put(`/admin/orders/${id}/ship`, { trackingNo })

// 配置管理
export const getAdminConfig = () =>
  request.get('/admin/config')

export const updateAdminConfig = (data: Record<string, string>) =>
  request.put('/admin/config', data)

// 统计看板
export const getAdminDashboard = () =>
  request.get<DashboardVO>('/admin/dashboard')

// ===== 数据导出 =====
export const exportOrdersCsv = (params?: { status?: number; orderNo?: string }) => {
  const token = localStorage.getItem('token')
  let url = '/api/admin/orders/export?'
  if (params) {
    if (params.status !== undefined) url += `status=${params.status}&`
    if (params.orderNo) url += `orderNo=${encodeURIComponent(params.orderNo)}&`
  }
  window.open(url + `token=${token}`, '_blank')
}

export const exportProductsCsv = (params?: { keyword?: string }) => {
  const token = localStorage.getItem('token')
  let url = '/api/admin/products/export?'
  if (params?.keyword) url += `keyword=${encodeURIComponent(params.keyword)}&`
  window.open(url + `token=${token}`, '_blank')
}

// ===== 客户积分管理 =====
export const searchUsers = (params: { keyword?: string; pageNum?: number; pageSize?: number }) =>
  request.get('/admin/users', { params })

export const adjustUserPoints = (userId: number, points: number, source: string, remark?: string) =>
  request.put(`/admin/users/${userId}/points`, { points, source, remark })

export const toggleUserStatus = (userId: number) =>
  request.put(`/admin/users/${userId}/status`)

// ===== 积分有效期 =====
export const getExpiredPoints = () =>
  request.get('/admin/expired-points')

export const cleanExpiredPoints = () =>
  request.post('/admin/points/clean-expired')
