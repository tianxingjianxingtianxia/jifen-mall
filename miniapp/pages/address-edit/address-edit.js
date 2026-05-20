// pages/address-edit/address-edit.js
const { request } = require('../../utils/request')

Page({
  data: {
    id: 0,
    isEdit: false,
    form: {
      receiverName: '',
      receiverPhone: '',
      province: '',
      city: '',
      district: '',
      detailAddress: '',
      isDefault: 0
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: Number(options.id), isEdit: true })
      this.loadAddress()
    }
  },

  async loadAddress() {
    try {
      const data = await request('/addresses')
      const addr = (data || []).find(a => a.id === this.data.id)
      if (addr) {
        this.setData({
          form: {
            receiverName: addr.receiverName,
            receiverPhone: addr.receiverPhone,
            province: addr.province,
            city: addr.city,
            district: addr.district,
            detailAddress: addr.detailAddress,
            isDefault: addr.isDefault
          }
        })
      }
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  toggleDefault(e) {
    this.setData({ 'form.isDefault': this.data.form.isDefault === 1 ? 0 : 1 })
  },

  async submit() {
    const f = this.data.form
    if (!f.receiverName) return wx.showToast({ title: '请填写收货人', icon: 'none' })
    if (!f.receiverPhone) return wx.showToast({ title: '请填写手机号', icon: 'none' })
    if (!f.province || !f.city || !f.district) return wx.showToast({ title: '请选择省市区', icon: 'none' })
    if (!f.detailAddress) return wx.showToast({ title: '请填写详细地址', icon: 'none' })

    try {
      if (this.data.isEdit) {
        await request(`/addresses/${this.data.id}`, 'PUT', this.data.form)
      } else {
        await request('/addresses', 'POST', this.data.form)
      }
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1000)
    } catch (err) {
      wx.showToast({ title: err.message || '保存失败', icon: 'none' })
    }
  }
})
