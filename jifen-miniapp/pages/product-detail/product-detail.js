import api from '../../utils/api';
import { showToast, showSuccess, showConfirm } from '../../utils/util';

Page({
  data: {
    product: null,
    loading: true,
    addresses: [],
    selectedAddressId: null,
    selectedAddress: null,
    points: 0,
  },

  onLoad(options) {
    const id = options.id;
    if (!id) {
      showToast('商品ID缺失');
      wx.navigateBack();
      return;
    }
    this.setData({ id });
    this.loadProduct(id);
    this.loadAddresses();
    this.loadPoints();
  },

  loadProduct(id) {
    this.setData({ loading: true });
    api.get('/products/' + id).then(data => {
      this.setData({ product: data, loading: false });
    }).catch(err => {
      showToast(err.message || '加载失败');
      this.setData({ loading: false });
    });
  },

  loadAddresses() {
    api.get('/addresses').then(data => {
      const list = data.list || data.records || data || [];
      const defaultAddr = list.find(a => a.isDefault) || list[0] || null;
      this.setData({
        addresses: list,
        selectedAddress: defaultAddr,
        selectedAddressId: defaultAddr ? defaultAddr.id : null,
      });
    }).catch(() => {});
  },

  loadPoints() {
    api.get('/points/balance').then(data => {
      this.setData({ points: data.points || 0 });
    }).catch(() => {});
  },

  // 选择收货地址
  onSelectAddress() {
    const list = this.data.addresses;
    if (!list || list.length === 0) {
      showToast('请先添加收货地址');
      wx.navigateTo({ url: '/pages/address-edit/address-edit' });
      return;
    }
    const items = list.map(a => ({
      name: a.name + ' ' + a.phone,
      address: (a.province || '') + (a.city || '') + (a.district || '') + (a.detailAddress || ''),
    }));
    wx.showActionSheet({
      itemList: list.map((a, i) => a.name + ' ' + a.phone),
      success: (res) => {
        const addr = list[res.tapIndex];
        this.setData({
          selectedAddress: addr,
          selectedAddressId: addr.id,
        });
      }
    });
  },

  // 立即兑换
  onExchange() {
    const { product, selectedAddressId, selectedAddress } = this.data;
    if (!product) return;

    if (!selectedAddressId) {
      showToast('请选择收货地址');
      return;
    }

    if (product.stock < 1) {
      showToast('库存不足');
      return;
    }

    showConfirm('兑换确认', '确认用 ' + product.pointsRequired + ' 积分兑换该商品？')
      .then(() => {
        wx.showLoading({ title: '兑换中...' });
        return api.post('/orders', {
          productId: product.id,
          addressId: selectedAddressId,
        });
      })
      .then(data => {
        wx.hideLoading();
        showSuccess('兑换成功');
        wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + (data.id || data.orderId) });
      })
      .catch(err => {
        wx.hideLoading();
        if (err && err !== false) {
          showToast(err.message || '兑换失败');
        }
      });
  },

  // 预览图片
  onPreviewImage(e) {
    const idx = e.currentTarget.dataset.index;
    const urls = this.data.product.images || [];
    wx.previewImage({
      current: urls[idx],
      urls,
    });
  },
});
