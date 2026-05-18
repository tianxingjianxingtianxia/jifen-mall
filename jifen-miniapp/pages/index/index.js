import api from '../../utils/api';
import config from '../../utils/config';
import { showToast, showSuccess } from '../../utils/util';

const PAGE_SIZE = 10;

Page({
  data: {
    points: 0,
    totalEarned: 0,
    totalSpent: 0,
    todaySigned: false,
    signing: false,
    keyword: '',
    sortBy: '',
    pageNum: 1,
    hasMore: true,
    loading: false,
    products: [],
    sortOptions: [
      { value: '', label: '默认排序' },
      { value: 'pointsRequired', label: '积分最低' },
      { value: 'saleCount', label: '销量最高' },
    ],
    sortIndex: 0,
    nickname: '用户',
    firstChar: '用',
  },

  onLoad() {
    // 从缓存读取用户信息
    this.loadFromCache();
    // 加载商品列表（不需要 token）
    this.loadProducts(true);
  },

  onShow() {
    // 每次显示时刷新
    this.loadFromCache();
    this.loadProducts(true);
  },

  loadFromCache() {
    try {
      const info = wx.getStorageSync('userInfo');
      if (info) {
        const u = typeof info === 'string' ? JSON.parse(info) : info;
        this.setData({
          nickname: u.nickname || u.username || '用户',
          firstChar: (u.nickname || u.username || '用').substring(0, 1),
          points: u.points || 0,
        });
      }
    } catch (e) {}
  },

  loadProducts(reset = false) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });

    api.get('/products', {
      keyword: this.data.keyword,
      sortBy: this.data.sortBy,
      pageNum,
      pageSize: PAGE_SIZE,
    }).then(data => {
      const list = data.list || data.records || [];
      this.setData({
        products: reset ? list : this.data.products.concat(list),
        pageNum,
        hasMore: list.length >= PAGE_SIZE,
        loading: false,
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  onSearchInput(e) { this.setData({ keyword: e.detail.value }); },
  onSearch() { this.loadProducts(true); },

  onGoDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + id });
  },

  onGoProfile() {
    wx.switchTab({ url: '/pages/profile/profile' });
  },

  onSortChange(e) {
    const idx = e.detail.value;
    const sortBy = this.data.sortOptions[idx].value;
    this.setData({ sortIndex: idx, sortBy });
    this.loadProducts(true);
  },

  onSignIn() {
    if (this.data.todaySigned || this.data.signing) return;
    this.setData({ signing: true });
    api.post('/points/sign-in').then(data => {
      showSuccess('签到成功 +' + (data.points || 0) + '积分');
      this.setData({
        todaySigned: true,
        points: this.data.points + (data.points || 0)
      });
    }).catch(err => {
      showToast(err.message || '签到失败');
    }).finally(() => {
      this.setData({ signing: false });
    });
  },

  onReachBottom() {
    this.loadProducts(false);
  }
});
