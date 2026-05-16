import request from '@/utils/request'

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
}

export interface OrderItem {
  id: number
  orderNo: string
  userName: string
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
