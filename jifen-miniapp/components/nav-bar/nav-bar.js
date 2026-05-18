// 顶部导航栏组件
Component({
  properties: {
    title: { type: String, value: '积分商城' },
    showBack: { type: Boolean, value: true },
    backgroundColor: { type: String, value: 'linear-gradient(135deg, #667eea, #764ba2)' },
  },

  data: {
    statusBarHeight: 20,
    navBarHeight: 44,
  },

  lifetimes: {
    attached() {
      const sys = wx.getSystemInfoSync();
      const menu = wx.getMenuButtonBoundingClientRect();
      this.setData({
        statusBarHeight: sys.statusBarHeight,
        navBarHeight: menu.top ? menu.top + menu.height + 8 : 44,
      });
    }
  },

  methods: {
    onBack() {
      if (getCurrentPages().length > 1) {
        wx.navigateBack();
      } else {
        wx.switchTab({ url: '/pages/index/index' });
      }
    }
  }
});
