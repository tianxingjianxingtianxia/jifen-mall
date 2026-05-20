App({
  onLaunch() {
    // 检查登录态
    const token = wx.getStorageSync('token')
    if (!token) {
      // 未登录，后续在首页 onShow 中弹出登录
      this.globalData.isLoggedIn = false
    } else {
      this.globalData.isLoggedIn = true
    }
  },

  globalData: {
    isLoggedIn: false,
    userInfo: null
  },

  // 检查登录，未登录弹出登录页
  checkLogin() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.navigateTo({ url: '/pages/login/login' })
      return false
    }
    return true
  },

  // 登录成功后设置
  setLogin(token, userInfo) {
    wx.setStorageSync('token', token)
    wx.setStorageSync('userInfo', userInfo)
    this.globalData.isLoggedIn = true
    this.globalData.userInfo = userInfo
  },

  // 退出登录
  logout() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    this.globalData.isLoggedIn = false
    this.globalData.userInfo = null
  }
})
