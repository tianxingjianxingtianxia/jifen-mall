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

      // 检测微信 session 是否有效
      wx.checkSession({
        success() {
          // session 有效，直接使用已有 token
          console.log('微信 session 有效，使用已有 token');
        },
        fail() {
          // session 失效，调用 wx.login 重新获取
          console.log('微信 session 已失效，重新登录');
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          // 重新静默登录
          wx.login({
            success(res) {
              if (res.code) {
                const app = getApp();
                wx.request({
                  url: require('./utils/config').default.API_BASE_URL + '/auth/wx-login',
                  method: 'POST',
                  data: { code: res.code },
                  header: { 'Content-Type': 'application/json' },
                  success(loginRes) {
                    const data = loginRes.data;
                    if (data.code === 200) {
                      app.onLoginSuccess(data.data.token, data.data);
                    }
                  }
                });
              }
            }
          });
        }
      });
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
