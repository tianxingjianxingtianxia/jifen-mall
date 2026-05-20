// pages/login/login.js
const { wxLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    loading: false,
    // 开发测试：用户名密码登录
    showDev: true,
    devMode: 'login',  // 'login' | 'register'
    username: '',
    password: '',
    nickname: '',
    phone: ''
  },

  // 微信授权登录
  async handleLogin() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const userInfo = await wxLogin()
      const app = getApp()
      app.setLogin(userInfo.token, {
        userId: userInfo.userId,
        username: userInfo.username,
        nickname: userInfo.nickname,
        points: userInfo.points
      })
      wx.reLaunch({ url: '/pages/index/index' })
    } catch (err) {
      wx.showToast({ title: err.message || '登录失败', icon: 'none', duration: 2000 })
    } finally {
      this.setData({ loading: false })
    }
  },

  // === 开发测试登录 ===
  switchDevMode(e) {
    this.setData({ devMode: e.currentTarget.dataset.mode })
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  // 用户名密码登录
  async devLogin() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const data = await request('/auth/login', 'POST', {
        username: this.data.username,
        password: this.data.password
      }, { noAuth: true })
      const app = getApp()
      app.setLogin(data.token, {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        points: data.points || 0
      })
      wx.reLaunch({ url: '/pages/index/index' })
    } catch (err) {
      wx.showToast({ title: err.message || '登录失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  // 注册
  async devRegister() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '用户名和密码必填', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      await request('/auth/register', 'POST', {
        username: this.data.username,
        password: this.data.password,
        nickname: this.data.nickname || this.data.username,
        phone: this.data.phone || ''
      }, { noAuth: true })
      wx.showToast({ title: '注册成功，请登录', icon: 'success' })
      this.setData({ devMode: 'login' })
    } catch (err) {
      wx.showToast({ title: err.message || '注册失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
