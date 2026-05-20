// pages/points-records/points-records.js
const { request } = require('../../utils/request')

Page({
  data: {
    records: [],
    pageNum: 1,
    pageSize: 20,
    total: 0,
    loading: false,
    hasMore: true
  },

  onShow() {
    this.setData({ pageNum: 1, records: [], hasMore: true })
    this.loadRecords()
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.loadRecords(true)
  },

  async loadRecords(append = false) {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const data = await request(`/points/records?pageNum=${this.data.pageNum}&pageSize=${this.data.pageSize}`)
      const newRecords = append ? [...this.data.records, ...data.records] : data.records
      this.setData({
        records: newRecords,
        total: data.total,
        pageNum: data.pageNum + 1,
        hasMore: newRecords.length < data.total,
        loading: false
      })
    } catch (err) {
      this.setData({ loading: false })
    }
  }
})
