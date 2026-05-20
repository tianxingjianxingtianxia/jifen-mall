// pages/index/index.js
const { request } = require('../../utils/request')
const { isLoggedIn } = require('../../utils/auth')
const { getUserInfo, setUserInfo } = require('../../utils/storage')

Page({
  data: {
    // 积分信息
    points: 0,
    totalEarned: 0,
    totalSpent: 0,
    todaySigned: false,

    // 商品列表
    products: [],
    productRows: [],
    keyword: '',
    sortBy: '',       // '' | 'points_asc' | 'points_desc'
    pageNum: 1,
    pageSize: 12,
    total: 0,
    loading: false,
    hasMore: true,

    // 搜索
    searchValue: ''
  },

  onShow() {
    // 检查登录态
    if (!isLoggedIn()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }

    // 刷新数据
    this.loadBalance()
    this.checkTodaySign()
    this.loadProducts()
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({ pageNum: 1, products: [], hasMore: true })
    Promise.all([
      this.loadBalance(),
      this.checkTodaySign(),
      this.loadProducts()
    ]).then(() => wx.stopPullDownRefresh())
  },

  // 触底加载更多
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadProducts(true)
    }
  },

  // 加载积分余额
  async loadBalance() {
    try {
      const data = await request('/points/balance')
      this.setData({
        points: data.points,
        totalEarned: data.totalEarned,
        totalSpent: data.totalSpent
      })
    } catch (err) {
      console.error('加载余额失败:', err)
    }
  },

  // 检查今日签到
  async checkTodaySign() {
    try {
      const signed = await request('/points/today-sign')
      this.setData({ todaySigned: signed })
    } catch (err) {
      console.error('检查签到失败:', err)
    }
  },

  // 签到
  async handleSignIn() {
    if (this.data.todaySigned) {
      wx.showToast({ title: '今日已签到', icon: 'none' })
      return
    }
    try {
      const data = await request('/points/sign-in', 'POST')
      wx.showToast({ title: `签到成功 +${data.points}积分`, icon: 'none' })
      this.setData({
        todaySigned: true,
        points: data.totalPoints
      })
    } catch (err) {
      wx.showToast({ title: err.message || '签到失败', icon: 'none' })
    }
  },

  // 搜索
  onSearchConfirm(e) {
    const value = (e.detail && e.detail.value) || this.data.searchValue
    this.setData({ keyword: value, pageNum: 1, products: [], hasMore: true })
    this.loadProducts()
  },
  onSearchInput(e) {
    this.setData({ searchValue: e.detail.value })
  },

  // 排序切换
  onSortChange(e) {
    const sortBy = e.currentTarget.dataset.sort
    this.setData({ sortBy, pageNum: 1, products: [], hasMore: true })
    this.loadProducts()
  },

  // 加载商品列表
  async loadProducts(append = false) {
    if (!append) {
      this.setData({ pageNum: 1, hasMore: true })
    }
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const params = {
        pageNum: this.data.pageNum,
        pageSize: this.data.pageSize
      }
      if (this.data.keyword) params.keyword = this.data.keyword
      if (this.data.sortBy) params.sortBy = this.data.sortBy

      const data = await request('/products?' + this.toQuery(params))
      const newProducts = append ? [...this.data.products, ...data.records] : data.records
      // 双列布局：每行2个
      const productRows = []
      for (let i = 0; i < newProducts.length; i += 2) {
        productRows.push({
          left: newProducts[i],
          right: newProducts[i + 1] || null
        })
      }
      this.setData({
        products: newProducts,
        productRows,
        total: data.total,
        pageNum: data.pageNum + 1,
        hasMore: newProducts.length < data.total,
        loading: false
      })
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' })
      this.setData({ loading: false })
    }
  },

  // 对象转 query string
  toQuery(obj) {
    return Object.keys(obj)
      .filter(k => obj[k] !== undefined && obj[k] !== null && obj[k] !== '')
      .map(k => `${k}=${encodeURIComponent(obj[k])}`)
      .join('&')
  },

  // 跳转商品详情
  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product-detail/product-detail?id=${id}` })
  }
})
