import api from '../../utils/api';
import { showToast, showConfirm, showSuccess } from '../../utils/util';

Page({
  data: {
    userInfo: null,
    points: 0,
    avatarFirst: '用',
  },

  onLoad() {
    this.loadUserInfo();
    this.loadPoints();
  },

  onShow() {
    this.loadUserInfo();
    this.loadPoints();
  },

  loadUserInfo() {
    const cache = wx.getStorageSync('userInfo');
    if (cache) {
      try {
        this.setData({ userInfo: JSON.parse(cache) });
      } catch (e) {
        this.setData({ userInfo: cache });
      }
    }
    // 尝试从接口获取最新
    api.get('/user/info').then(data => {
      const nick = data.nickname || data.username || '用户'
      this.setData({
        userInfo: data,
        nickname: nick,
        avatarFirst: nick.substring(0, 1),
      });
      wx.setStorageSync('userInfo', JSON.stringify(data));
    }).catch(() => {});
  },

  loadPoints() {
    api.get('/points/balance').then(data => {
      this.setData({ points: data.points || 0 });
    }).catch(() => {});
  },

  // 跳转到订单
  goToOrders() {
    wx.switchTab({ url: '/pages/orders/orders' });
  },

  // 跳转到积分明细
  onGoRecords() {
    wx.navigateTo({ url: '/pages/points-records/points-records' });
  },
  goToPointsRecords() {
    wx.navigateTo({ url: '/pages/points-records/points-records' });
  },

  // 跳转到地址管理
  onGoAddresses() {
    wx.navigateTo({ url: '/pages/addresses/addresses' });
  },
  goToAddresses() {
    wx.navigateTo({ url: '/pages/addresses/addresses' });
  },

  // 退出登录
  onLogout() {
    showConfirm('提示', '确定要退出登录吗？').then(() => {
      wx.clearStorageSync();
      showSuccess('已退出');
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/index/index' });
      }, 1000);
    }).catch(() => {});
  },
});
