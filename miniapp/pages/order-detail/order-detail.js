// pages/order-detail/order-detail.js
const { request } = require('../../utils/request')

Page({
  data: { order: null, id: 0 },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: Number(options.id) })
      this.loadDetail()
    }
  },

  async loadDetail() {
    try {
      const data = await request(`/orders/${this.data.id}`)
      this.setData({ order: data })
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  async cancelOrder() {
    const res = await new Promise(r => wx.showModal({ title: '确认取消', content: '取消后积分退回', success: r }))
    if (!res.confirm) return
    try {
      await request(`/orders/${this.data.id}/cancel`, 'POST')
      wx.showToast({ title: '已取消', icon: 'success' })
      this.loadDetail()
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' })
    }
  },

  async confirmOrder() {
    try {
      await request(`/orders/${this.data.id}/confirm`, 'POST')
      wx.showToast({ title: '已收货', icon: 'success' })
      this.loadDetail()
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' })
    }
  }
})
