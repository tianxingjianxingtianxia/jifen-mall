// 订单卡片组件
import { getOrderStatusText, getOrderStatusType, formatTime } from '../../utils/util';

Component({
  properties: {
    order: { type: Object, value: {} }
  },

  data: {
    statusText: '',
    statusType: '',
    createTime: '',
  },

  observers: {
    'order'(val) {
      if (val && val.status !== undefined) {
        this.setData({
          statusText: getOrderStatusText(val.status),
          statusType: getOrderStatusType(val.status),
          createTime: formatTime(val.createTime),
        });
      }
    }
  },

  methods: {
    onClick() {
      this.triggerEvent('click', { id: this.data.order.id });
    },
    onCancel(e) {
      e.stopPropagation?.();
      this.triggerEvent('cancel', { id: this.data.order.id });
    },
    onConfirm(e) {
      e.stopPropagation?.();
      this.triggerEvent('confirm', { id: this.data.order.id });
    }
  }
});
