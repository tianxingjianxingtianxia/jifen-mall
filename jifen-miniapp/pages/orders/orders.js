import api from '../../utils/api';
import { showToast, showSuccess, showConfirm } from '../../utils/util';

const PAGE_SIZE = 10;

Page({
  data: {
    tabs: ['全部', '待发货', '已发货', '已完成', '已取消'],
    statusList: ['', '0', '1', '2', '3'],
    activeTab: 0,
    orders: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
  },

  onLoad() {
    this.loadOrders(true);
  },

  onShow() {
    this.loadOrders(true);
  },

  // Tab 切换
  onTabChange(e) {
    const index = e.currentTarget.dataset.index;
    if (index === this.data.activeTab) return;
    this.setData({ activeTab: index });
    this.loadOrders(true);
  },

  // 加载订单列表
  loadOrders(reset = false) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;

    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });

    const status = this.data.statusList[this.data.activeTab];

    api.get('/orders', {
      status,
      pageNum,
      pageSize: PAGE_SIZE,
    }).then(data => {
      const list = data.list || data.records || [];
      this.setData({
        orders: reset ? list : this.data.orders.concat(list),
        pageNum,
        hasMore: list.length >= PAGE_SIZE,
        loading: false,
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  // 点击订单
  onOrderClick(e) {
    const id = e.detail.id;
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + id });
  },

  // 取消订单
  onOrderCancel(e) {
    const id = e.detail.id;
    showConfirm('确认取消', '确定取消该订单吗？').then(() => {
      wx.showLoading({ title: '操作中...' });
      return api.post('/orders/' + id + '/cancel');
    }).then(() => {
      wx.hideLoading();
      showSuccess('已取消');
      this.loadOrders(true);
    }).catch(err => {
      wx.hideLoading();
      if (err && err !== false) {
        showToast(err.message || '操作失败');
      }
    });
  },

  // 确认收货
  onOrderConfirm(e) {
    const id = e.detail.id;
    showConfirm('确认收货', '确定已收到商品吗？').then(() => {
      wx.showLoading({ title: '操作中...' });
      return api.post('/orders/' + id + '/confirm');
    }).then(() => {
      wx.hideLoading();
      showSuccess('已确认收货');
      this.loadOrders(true);
    }).catch(err => {
      wx.hideLoading();
      if (err && err !== false) {
        showToast(err.message || '操作失败');
      }
    });
  },

  // 触底加载
  onReachBottom() {
    this.loadOrders();
  },
});
