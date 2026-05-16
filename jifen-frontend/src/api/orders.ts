import request from '@/utils/request'

export interface OrderVO {
  id: number
  orderNo: string
  productId: number
  productName: string
  productImage: string
  pointsSpent: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  status: number
  statusText: string
  trackingNo: string
  cancelReason: string
  expireTime: string
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export const createOrder = (data: { productId: number; addressId: number }) => {
  return request.post<OrderVO>('/orders', data)
}

export const getOrders = (params: { status?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<PageResult<OrderVO>>('/orders', { params })
}

export const getOrderDetail = (id: number) => {
  return request.get<OrderVO>(`/orders/${id}`)
}

export const cancelOrder = (id: number) => {
  return request.post(`/orders/${id}/cancel`)
}

export const confirmReceipt = (id: number) => {
  return request.post(`/orders/${id}/confirm`)
}

export const STATUS_MAP: Record<number, string> = {
  0: '待发货',
  1: '已发货',
  2: '已完成',
  3: '已取消',
}
