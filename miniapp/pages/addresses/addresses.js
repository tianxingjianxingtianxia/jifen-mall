// pages/addresses/addresses.js
const { request } = require('../../utils/request')

Page({
  data: { addresses: [] },

  onShow() { this.loadAddresses() },

  async loadAddresses() {
    try {
      const data = await request('/addresses')
      this.setData({ addresses: data || [] })
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  goEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/address-edit/address-edit?id=${id || ''}` })
  },

  async setDefault(e) {
    const id = e.currentTarget.dataset.id
    try {
      await request(`/addresses/${id}/default`, 'PUT')
      this.loadAddresses()
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' })
    }
  },

  async deleteAddress(e) {
    const id = e.currentTarget.dataset.id
    const res = await new Promise(r => wx.showModal({ title: '确认删除', content: '删除后不可恢复', success: r }))
    if (!res.confirm) return
    try {
      await request(`/addresses/${id}`, 'DELETE')
      wx.showToast({ title: '已删除', icon: 'success' })
      this.loadAddresses()
    } catch (err) {
      wx.showToast({ title: err.message, icon: 'none' })
    }
  },

  goAdd() {
    wx.navigateTo({ url: '/pages/address-edit/address-edit' })
  }
})
