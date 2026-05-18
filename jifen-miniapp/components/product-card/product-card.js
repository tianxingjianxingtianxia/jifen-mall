// 商品卡片组件
Component({
  properties: {
    product: { type: Object, value: {} }
  },

  methods: {
    onClick() {
      const pid = this.data.product.id;
      wx.navigateTo({ url: '/pages/product-detail/product-detail?id=' + pid });
    }
  }
});
