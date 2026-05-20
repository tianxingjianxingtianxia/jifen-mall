// pages/orders/orders.js
const { request } = require('../../utils/request')
const { ORDER_STATUS } = require('../../utils/config')

const STATUS_TABS = [
  { label: '全部', value: '' },
  { label: '待发货', value: '0' },
  { label: '已发货', value: '1' },
  { label: '已完成', value: '2' },
  { label: '已取消', value: '3' }
]

Page({
  data: {
    tabs: STATUS_TABS,
    activeTab: '',
    orders: [],
    pageNum: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    hasMore: true
  },

  onShow() {
    this.setData({ pageNum: 1, orders: [], hasMore: true })
    this.loadOrders()
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadOrders(true)
    }
  },

  onTabChange(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ activeTab: value, pageNum: 1, orders: [], hasMore: true })
    this.loadOrders()
  },

  async loadOrders(append = false) {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const params = { pageNum: this.data.pageNum, pageSize: this.data.pageSize }
      if (this.data.activeTab !== '') params.status = Number(this.data.activeTab)
      const qs = Object.keys(params).map(k => `${k}=${params[k]}`).join('&')
      const data = await request(`/orders?${qs}`)
      const newOrders = append ? [...this.data.orders, ...data.records] : data.records
      this.setData({
        orders: newOrders,
        total: data.total,
        pageNum: data.pageNum + 1,
        hasMore: newOrders.length < data.total,
        loading: false
      })
    } catch (err) {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` })
  },

  async cancelOrder(e) {
    const id = e.currentTarget.dataset.id
    const res = await new Promise(r => wx.showModal({ title: '确认取消', content: '取消后积分退回', success: r }))
    if (!res.confirm) return
    try {
      await request(`/orders/${id}/cancel`, 'POST')
      wx.showToast({ title: '已取消', icon: 'success' })
      this.setData({ pageNum: 1, orders: [], hasMore: true })
      this.loadOrders()
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' })
    }
  },

  async confirmOrder(e) {
    const id = e.currentTarget.dataset.id
    try {
      await request(`/orders/${id}/confirm`, 'POST')
      wx.showToast({ title: '已收货', icon: 'success' })
      this.setData({ pageNum: 1, orders: [], hasMore: true })
      this.loadOrders()
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' })
    }
  }
})
