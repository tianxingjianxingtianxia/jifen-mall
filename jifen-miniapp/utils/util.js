// 工具函数

// 格式化时间
function formatTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  const s = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}:${s}`;
}

// 格式化日期（无时间）
function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}/${m}/${d}`;
}

// 订单状态文本
function getOrderStatusText(status) {
  const map = {
    0: '待发货', 1: '已发货', 2: '已完成', 3: '已取消'
  };
  return map[status] || '未知';
}

// 订单状态 tag 类型
function getOrderStatusType(status) {
  const map = {
    0: 'warning', 1: 'primary', 2: 'success', 3: 'info'
  };
  return map[status] || 'default';
}

// 积分类型文本
function getPointTypeText(type) {
  return type === 1 ? '获得' : '消耗';
}

// 积分来源文本
function getPointSourceText(source) {
  const map = {
    'SIGN_IN': '每日签到',
    'EXCHANGE': '商品兑换',
    'ORDER_CANCEL': '取消订单退回',
    'MANUAL_ADJUST': '手动调整',
    '转介绍签约': '转介绍签约',
    '售后回访': '售后回访',
    '手动调整': '手动调整',
  };
  return map[source] || source || '其他';
}

// 显示 Toast
function showToast(title, icon = 'none') {
  wx.showToast({ title, icon, duration: 2000 });
}

// 显示成功 Toast
function showSuccess(title) {
  wx.showToast({ title, icon: 'success', duration: 2000 });
}

// 显示确认弹窗
function showConfirm(title, content) {
  return new Promise((resolve, reject) => {
    wx.showModal({
      title,
      content,
      success(res) {
        if (res.confirm) resolve(true);
        else reject(false);
      },
      fail: reject
    });
  });
}

export {
  formatTime, formatDate,
  getOrderStatusText, getOrderStatusType,
  getPointTypeText, getPointSourceText,
  showToast, showSuccess, showConfirm,
};
