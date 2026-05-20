// pages/product-detail/product-detail.js
const { request } = require('../../utils/request')

Page({
  data: {
    id: 0,
    product: null,
    images: [],           // 轮播图
    addresses: [],        // 地址列表
    selectedAddress: null,// 选中的地址
    showExchange: false,  // 兑换弹窗
    exchanging: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: Number(options.id) })
      this.loadDetail()
      this.loadAddresses()
    }
  },

  async loadDetail() {
    try {
      const data = await request(`/products/${this.data.id}`)
      this.setData({
        product: data,
        images: data.images && data.images.length > 0 ? data.images : [data.coverImage]
      })
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  async loadAddresses() {
    try {
      const data = await request('/addresses')
      this.setData({ addresses: data || [] })
    } catch (err) {
      console.error('加载地址失败:', err)
    }
  },

  // 打开兑换弹窗
  openExchange() {
    if (!this.data.product || this.data.product.status !== 1) {
      wx.showToast({ title: '商品已下架', icon: 'none' })
      return
    }
    if (this.data.product.stock <= 0) {
      wx.showToast({ title: '库存不足', icon: 'none' })
      return
    }
    if (this.data.addresses.length === 0) {
      wx.showModal({
        title: '提示',
        content: '请先添加收货地址',
        confirmText: '去添加',
        success: res => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/address-edit/address-edit' })
          }
        }
      })
      return
    }
    this.setData({ showExchange: true })
  },

  // 选择地址
  selectAddress(e) {
    const index = e.currentTarget.dataset.index
    this.setData({ selectedAddress: this.data.addresses[index] })
  },

  // 确认兑换
  async confirmExchange() {
    if (!this.data.selectedAddress) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }
    if (this.data.exchanging) return
    this.setData({ exchanging: true })

    try {
      const data = await request('/orders', 'POST', {
        productId: this.data.id,
        addressId: this.data.selectedAddress.id
      })
      wx.showToast({ title: '兑换成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/order-detail/order-detail?id=${data.id}` })
      }, 1000)
    } catch (err) {
      wx.showToast({ title: err.message || '兑换失败', icon: 'none' })
    } finally {
      this.setData({ exchanging: false, showExchange: false })
    }
  },

  closeExchange() {
    this.setData({ showExchange: false })
  },

  // 跳转新增地址
  goAddAddress() {
    wx.navigateTo({ url: '/pages/address-edit/address-edit' })
  }
})
