import request from '../utils/request'

export interface ProductItem {
  id: number
  name: string
  coverImage: string
  pointsRequired: number
  stock: number
  saleCount: number
  stockStatus: string
}

export interface ProductDetail {
  id: number
  name: string
  description: string
  coverImage: string
  pointsRequired: number
  stock: number
  status: number
  sortOrder: number
  saleCount: number
  images: string[]
  stockStatus: string
}

export interface ProductPage {
  records: ProductItem[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 商品列表（分页） */
export function getProducts(params: {
  keyword?: string
  sortBy?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<ProductPage>('/products', { params }) as Promise<ProductPage>
}

/** 商品详情 */
export function getProductDetail(id: number) {
  return request.get<ProductDetail>(`/products/${id}`) as Promise<ProductDetail>
}
