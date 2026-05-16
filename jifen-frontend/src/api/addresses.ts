import request from '../utils/request'

export interface Address {
  id: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
}

export interface AddressInput {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: number
}

/** 获取地址列表 */
export function getAddresses() {
  return request.get<Address[]>('/addresses') as Promise<Address[]>
}

/** 新增地址 */
export function addAddress(data: AddressInput) {
  return request.post<Address>('/addresses', data) as Promise<Address>
}

/** 更新地址 */
export function updateAddress(id: number, data: AddressInput) {
  return request.put<Address>(`/addresses/${id}`, data) as Promise<Address>
}

/** 删除地址 */
export function deleteAddress(id: number) {
  return request.delete<void>(`/addresses/${id}`) as Promise<void>
}

/** 设为默认地址 */
export function setDefaultAddress(id: number) {
  return request.put<void>(`/addresses/${id}/default`) as Promise<void>
}
