// 积分卡片组件
Component({
  properties: {
    points: { type: Number, value: 0 },
    totalEarned: { type: Number, value: 0 },
    totalSpent: { type: Number, value: 0 },
    todaySigned: { type: Boolean, value: false },
    signing: { type: Boolean, value: false },
  },

  methods: {
    onSignIn() {
      this.triggerEvent('signin');
    }
  }
});
