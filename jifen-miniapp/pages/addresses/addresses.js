import api from '../../utils/api';
import { showToast, showSuccess, showConfirm } from '../../utils/util';

Page({
  data: {
    addresses: [],
    loading: true,
  },

  onLoad() {
    this.loadAddresses();
  },

  onShow() {
    this.loadAddresses();
  },

  loadAddresses() {
    this.setData({ loading: true });
    api.get('/addresses').then(data => {
      const list = data.list || data.records || data || [];
      this.setData({ addresses: list, loading: false });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  // 新增地址
  onAdd() {
    wx.navigateTo({ url: '/pages/address-edit/address-edit' });
  },

  // 编辑地址
  onEdit(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/address-edit/address-edit?id=' + id });
  },

  // 设为默认
  onSetDefault(e) {
    const id = e.currentTarget.dataset.id;
    wx.showLoading({ title: '设置中...' });
    api.put('/addresses/' + id + '/default').then(() => {
      wx.hideLoading();
      showSuccess('已设为默认');
      this.loadAddresses();
    }).catch(err => {
      wx.hideLoading();
      showToast(err.message || '设置失败');
    });
  },

  // 删除地址
  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    showConfirm('确认删除', '确定要删除该地址吗？').then(() => {
      wx.showLoading({ title: '删除中...' });
      return api.del('/addresses/' + id);
    }).then(() => {
      wx.hideLoading();
      showSuccess('已删除');
      this.loadAddresses();
    }).catch(err => {
      wx.hideLoading();
      if (err && err !== false) {
        showToast(err.message || '删除失败');
      }
    });
  },
});
