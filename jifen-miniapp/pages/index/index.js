import api from '../../utils/api';
import { showToast, showSuccess, showConfirm } from '../../utils/util';

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
  },

  onLoad() {
    this.loadProducts(true);
    // 积分和签到在登录后加载
    const token = wx.getStorageSync('token');
    if (token) {
      this.loadPointsBalance();
      this.loadTodaySign();
    }
  },

  onShow() {
    this.loadProducts(true);
    setTimeout(() => {
      this.loadPointsBalance();
      this.loadTodaySign();
    }, 500);
  },

  // 加载积分余额
  loadPointsBalance() {
    api.get('/points/balance').then(data => {
      this.setData({
        points: data.points,
        totalEarned: data.totalEarned,
        totalSpent: data.totalSpent,
      });
    }).catch(() => {});
  },

  // 加载签到状态
  loadTodaySign() {
    api.get('/points/today-sign').then(data => {
      this.setData({ todaySigned: data.signed });
    }).catch(() => {});
  },

  // 加载商品列表
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
      // 拼接完整图片 URL
      const API_BASE = require('../../utils/config').default.API_BASE_URL;
      const baseUrl = API_BASE.replace('/api', '');
      list.forEach(item => {
        if (item.coverImage && !item.coverImage.startsWith('http')) {
          item._coverImage = baseUrl + item.coverImage;
        } else {
          item._coverImage = item.coverImage;
        }
        if (!item._coverImage) item._coverImage = '/assets/images/default.png';
      });
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

  // 搜索
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.loadProducts(true);
  },

  // 跳转商品详情
  onGoDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + id });
  },

  // 跳转个人中心
  onGoProfile() {
    wx.switchTab({ url: '/pages/profile/profile' });
  },

  // 排序选择
  onSortChange(e) {
    const idx = e.detail.value;
    const sortBy = this.data.sortOptions[idx].value;
    this.setData({ sortIndex: idx, sortBy });
    this.loadProducts(true);
  },

  // 签到
  onSignIn() {
    if (this.data.todaySigned || this.data.signing) return;
    this.setData({ signing: true });
    api.post('/points/sign-in').then(data => {
      showSuccess('签到成功 +' + (data.points || 0) + '积分');
      this.setData({ todaySigned: true, points: this.data.points + (data.points || 0) });
      this.loadPointsBalance();
    }).catch(err => {
      showToast(err.message || '签到失败');
    }).finally(() => {
      this.setData({ signing: false });
    });
  },

  // 触底加载
  onReachBottom() {
    this.loadProducts();
  },
});
