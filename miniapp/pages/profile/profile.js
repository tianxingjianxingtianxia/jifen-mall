// pages/profile/profile.js
const { getUserInfo } = require('../../utils/storage')

Page({
  data: {
    userInfo: null,
    points: 0
  },

  onShow() {
    const info = getUserInfo()
    if (info) {
      this.setData({ userInfo: info, points: info.points || 0 })
    }
  },

  goAddresses() {
    wx.navigateTo({ url: '/pages/addresses/addresses' })
  },

  goPointsRecords() {
    wx.navigateTo({ url: '/pages/points-records/points-records' })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出吗？',
      success: res => {
        if (res.confirm) {
          const app = getApp()
          app.logout()
          wx.reLaunch({ url: '/pages/index/index' })
        }
      }
    })
  }
})
