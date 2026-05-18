import api from '../../utils/api';
import { getPointTypeText, getPointSourceText, formatTime, showToast } from '../../utils/util';

const PAGE_SIZE = 10;

Page({
  data: {
    records: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    _getPointTypeText: getPointTypeText,
    _getPointSourceText: getPointSourceText,
    _formatTime: formatTime,
  },

  onLoad() {
    setTimeout(() => {
      this.loadRecords(true);
    }, 500);
  },

  loadRecords(reset = false) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;

    const pageNum = reset ? 1 : this.data.pageNum + 1;
    this.setData({ loading: true });

    api.get('/points/records', {
      pageNum,
      pageSize: PAGE_SIZE,
    }).then(data => {
      const list = data.list || data.records || [];
      // 预处理显示字段
      const processed = list.map(item => ({
        ...item,
        typeText: getPointTypeText(item.type),
        sourceText: getPointSourceText(item.source),
        timeFormatted: formatTime(item.createTime),
      }));
      this.setData({
        records: reset ? processed : this.data.records.concat(processed),
        pageNum,
        hasMore: list.length >= PAGE_SIZE,
        loading: false,
      });
    }).catch(err => {
      showToast(err.message || '加载失败');
      this.setData({ loading: false });
    });
  },

  // 触底加载
  onReachBottom() {
    this.loadRecords();
  },
});
