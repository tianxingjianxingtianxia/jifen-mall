import api from '../../utils/api';
import { showToast, showSuccess } from '../../utils/util';

const PAGE_SIZE = 10;

Page({
  data: {
    points: 0,
    todaySigned: false,
    signing: false,
    keyword: '',
    sortBy: '',
    hasMore: true,
    loading: false,
    products: [],
    sortIndex: 0,
  },

  onLoad() {
    this.loadFromCache();
    this.loadProducts(true);
  },

  onShow() {
    this.loadFromCache();
  },

  loadFromCache() {
    try {
      const info = wx.getStorageSync('userInfo');
      if (info) {
        const u = typeof info === 'string' ? JSON.parse(info) : info;
        this.setData({ points: u.points || 0 });
      }
    } catch (e) {}
  },

  loadProducts(reset) {
    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });
    const self = this;
    wx.request({
      url: 'http://localhost:8080/api/products',
      data: { keyword: self.data.keyword, sortBy: self.data.sortBy, pageNum, pageSize: PAGE_SIZE },
      timeout: 5000,
      success(res) {
        const d = res.data;
        if (d.code === 200) {
          const list = d.data.records || [];
          self.setData({
            products: reset ? list : self.data.products.concat(list),
            pageNum,
            hasMore: list.length >= PAGE_SIZE,
            loading: false,
          });
        } else {
          self.setData({ loading: false });
        }
      },
      fail() {
        self.setData({ loading: false });
      }
    });
  },

  onSearch() { this.loadProducts(true); },
  onSearchInput(e) { this.setData({ keyword: e.detail.value }); },

  onGoDetail(e) {
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + e.currentTarget.dataset.id });
  },

  onSortChange(e) {
    const idx = e.detail.value;
    this.setData({ sortIndex: idx, sortBy: this.data.sortOptions[idx].value });
    this.loadProducts(true);
  },

  onSignIn() {
    if (this.data.todaySigned || this.data.signing) return;
    this.setData({ signing: true });
    api.post('/points/sign-in').then(data => {
      showSuccess('签到成功 +' + (data.points || 0));
      this.setData({ todaySigned: true, points: this.data.points + (data.points || 0) });
    }).catch(err => {
      showToast(err.message || '签到失败');
    }).finally(() => {
      this.setData({ signing: false });
    });
  },

  onReachBottom() { this.loadProducts(); }
});
