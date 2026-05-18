// 积分商城 - 小程序入口
import { checkLogin } from './utils/auth';

App({
  globalData: {
    userInfo: null,
    token: '',
  },

  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      try {
        const userInfo = wx.getStorageSync('userInfo');
        if (userInfo) {
          this.globalData.userInfo = JSON.parse(userInfo);
        }
      } catch (e) {}
    }
  },

  // 登录成功后的回调
  onLoginSuccess(token, userInfo) {
    this.globalData.token = token;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', token);
    wx.setStorageSync('userInfo', JSON.stringify(userInfo));
  },

  // 退出登录
  logout() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.reLaunch({ url: '/pages/index/index' });
  }
});
