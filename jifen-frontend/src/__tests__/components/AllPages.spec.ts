import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'

// ===== Mock all API modules =====
vi.mock('@/api/points', () => ({
  getTodaySign: vi.fn().mockResolvedValue(true),
  signIn: vi.fn().mockResolvedValue({ points: 20 }),
  getPointsBalance: vi.fn().mockResolvedValue({ points: 200, totalEarned: 210, totalSpent: 10 }),
}))

vi.mock('@/api/products', () => ({
  getProducts: vi.fn().mockResolvedValue({ records: [
    { id: 1, name: '测试商品', coverImage: '/img.jpg', pointsRequired: 50, stock: 10, stockStatus: '有货', saleCount: 5 }
  ], total: 1, pageNum: 1, pageSize: 12, pages: 1 }),
  getProductDetail: vi.fn().mockResolvedValue({
    id: 1, name: '测试商品', coverImage: '/img.jpg', pointsRequired: 50, stock: 10, stockStatus: '有货', saleCount: 5, description: '测试描述'
  }),
}))

vi.mock('@/api/orders', () => ({
  createOrder: vi.fn().mockResolvedValue({
    id: 1, orderNo: 'JF20260516123456', status: 0, statusText: '待发货', pointsSpent: 50
  }),
  getOrders: vi.fn().mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 }),
  cancelOrder: vi.fn().mockResolvedValue({}),
  confirmReceipt: vi.fn().mockResolvedValue({}),
  getOrderDetail: vi.fn().mockResolvedValue({
    id: 1, orderNo: 'JF20260516123456', status: 0, statusText: '待发货', productName: '测试商品', pointsSpent: 50
  }),
}))

vi.mock('@/api/addresses', () => ({
  getAddresses: vi.fn().mockResolvedValue([
    { id: 1, receiverName: '张三', receiverPhone: '13800138001', province: '广东省', city: '深圳市', district: '南山区', detailAddress: '科技园路', isDefault: 1 }
  ]),
  addAddress: vi.fn().mockResolvedValue({ id: 2 }),
  updateAddress: vi.fn().mockResolvedValue({}),
  deleteAddress: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/admin', () => ({
  getAdminProducts: vi.fn().mockResolvedValue({ records: [], total: 0 }),
  getAdminOrders: vi.fn().mockResolvedValue({ records: [], total: 0 }),
  getDashboard: vi.fn().mockResolvedValue({ totalUsers: 10, totalProducts: 5, totalOrders: 3, todaySignIns: 2 }),
  getConfig: vi.fn().mockResolvedValue([{ id: 1, configKey: 'sign_in_points', configValue: '10' }]),
  createProduct: vi.fn().mockResolvedValue(1),
  updateProductStatus: vi.fn().mockResolvedValue({}),
  shipOrder: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn().mockResolvedValue({ token: 'test-token', userId: 1, username: 'test', nickname: '测试' }),
  register: vi.fn().mockResolvedValue({ token: 'test-token', userId: 1 }),
  getUserInfo: vi.fn().mockResolvedValue({ userId: 1, username: 'test', nickname: '测试', points: 200 }),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    token: 'test-token',
    userInfo: { userId: 1, username: 'test', nickname: '测试', points: 200 },
    nickname: '测试',
    points: 200,
    isLoggedIn: true,
    setToken: vi.fn(),
    setUserInfo: vi.fn(),
    updatePoints: vi.fn(),
    fetchUserInfo: vi.fn(),
    logout: vi.fn(),
  }))
}))

// Mock vue-router
vi.mock('vue-router', () => ({
  useRoute: vi.fn(() => ({ params: { id: '1' } })),
  useRouter: vi.fn(() => ({
    push: vi.fn(),
    back: vi.fn(),
  })),
  createRouter: vi.fn(() => ({
    push: vi.fn(),
    back: vi.fn(),
  })),
  createWebHashHistory: vi.fn(),
  RouterLink: { template: '<a><slot /></a>' },
}))

describe('页面组件渲染测试', () => {
  it('Home.vue 能挂载', async () => {
    const Home = (await import('@/views/Home.vue')).default
    const wrapper = mount(Home, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('ProductDetail.vue 能挂载', async () => {
    const ProductDetail = (await import('@/views/ProductDetail.vue')).default
    const wrapper = mount(ProductDetail, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('Orders.vue 能挂载', async () => {
    const Orders = (await import('@/views/Orders.vue')).default
    const wrapper = mount(Orders, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('Addresses.vue 能挂载', async () => {
    const Addresses = (await import('@/views/Addresses.vue')).default
    const wrapper = mount(Addresses, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('Dashboard.vue 能挂载', async () => {
    const Dashboard = (await import('@/views/admin/Dashboard.vue')).default
    const wrapper = mount(Dashboard, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('AdminProducts.vue 能挂载', async () => {
    const Products = (await import('@/views/admin/Products.vue')).default
    const wrapper = mount(Products, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('AdminOrders.vue 能挂载', async () => {
    const AdminOrders = (await import('@/views/admin/Orders.vue')).default
    const wrapper = mount(AdminOrders, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('AdminConfig.vue 能挂载', async () => {
    const Config = (await import('@/views/admin/Config.vue')).default
    const wrapper = mount(Config, {
      global: { plugins: [] }
    })
    expect(wrapper.exists()).toBe(true)
    wrapper.unmount()
  })

  it('HomeHeader 导航栏显示导航链接', async () => {
    const Home = (await import('@/views/Home.vue')).default
    const wrapper = mount(Home, {
      global: { plugins: [] }
    })
    expect(wrapper.find('.header').exists()).toBe(true)
    wrapper.unmount()
  })

  it('积分卡片渲染', async () => {
    const Home = (await import('@/views/Home.vue')).default
    const wrapper = mount(Home, {
      global: { plugins: [] }
    })
    const card = wrapper.find('.points-card')
    expect(card.exists()).toBe(true)
    wrapper.unmount()
  })
})
