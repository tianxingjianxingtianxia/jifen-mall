import api from '../../utils/api';
import { showToast, showSuccess, showConfirm, getOrderStatusText, getOrderStatusType, formatTime } from '../../utils/util';

Page({
  data: {
    order: null,
    loading: true,
    statusText: '',
    createTimeFormatted: '',
  },

  onLoad(options) {
    const id = options.id;
    if (!id) {
      showToast('订单ID缺失');
      wx.navigateBack();
      return;
    }
    this.setData({ id });
    this.loadOrder(id);
  },

  loadOrder(id) {
    this.setData({ loading: true });
    api.get('/orders/' + id).then(data => {
      const statusText = getOrderStatusText(data.status);
      const createTimeFormatted = formatTime(data.createTime);
      // 处理时间线
      const timeline = (data.timeline || []).map(t => ({
        ...t,
        timeFormatted: formatTime(t.time),
      }));
      this.setData({
        order: data,
        statusText,
        createTimeFormatted,
        timeline,
        loading: false,
      });
    }).catch(err => {
      showToast(err.message || '加载失败');
      this.setData({ loading: false });
    });
  },

  // 取消订单
  onCancel() {
    showConfirm('确认取消', '确定取消该订单吗？').then(() => {
      wx.showLoading({ title: '操作中...' });
      return api.post('/orders/' + this.data.id + '/cancel');
    }).then(() => {
      wx.hideLoading();
      showSuccess('已取消');
      this.loadOrder(this.data.id);
    }).catch(err => {
      wx.hideLoading();
      if (err && err !== false) {
        showToast(err.message || '操作失败');
      }
    });
  },

  // 确认收货
  onConfirm() {
    showConfirm('确认收货', '确定已收到商品吗？').then(() => {
      wx.showLoading({ title: '操作中...' });
      return api.post('/orders/' + this.data.id + '/confirm');
    }).then(() => {
      wx.hideLoading();
      showSuccess('已确认收货');
      this.loadOrder(this.data.id);
    }).catch(err => {
      wx.hideLoading();
      if (err && err !== false) {
        showToast(err.message || '操作失败');
      }
    });
  },
});
