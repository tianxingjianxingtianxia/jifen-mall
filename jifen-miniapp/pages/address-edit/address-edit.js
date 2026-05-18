import api from '../../utils/api';
import { showToast, showSuccess } from '../../utils/util';

Page({
  data: {
    isEdit: false,
    id: null,
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false,
    provinces: [],
    cities: [],
    districts: [],
    provinceIndex: 0,
    cityIndex: 0,
    districtIndex: 0,
    // 省市区数据
    regionData: [],
    loading: false,
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ isEdit: true, id: options.id });
      this.loadAddress(options.id);
    }
    // 载入省市区数据
    this.loadRegionData();
  },

  loadAddress(id) {
    this.setData({ loading: true });
    api.get('/addresses/' + id).then(data => {
      this.setData({
        name: data.name || '',
        phone: data.phone || '',
        province: data.province || '',
        city: data.city || '',
        district: data.district || '',
        detailAddress: data.detailAddress || '',
        isDefault: data.isDefault || false,
        loading: false,
      });
    }).catch(err => {
      showToast(err.message || '加载失败');
      this.setData({ loading: false });
    });
  },

  // 加载省市区数据（简单模拟，实际项目中建议使用 region-picker 或静态数据）
  loadRegionData() {
    // 这里用占位数据，实际项目中可以从/api/regions获取或使用静态JSON
    // 简单起见，用 picker 的 range 自定义或改用 wx.chooseAddress
    try {
      const regions = require('../../utils/regions.js') || [];
      this.setData({ regionData: regions, provinces: regions.map(r => r.name) });
    } catch (e) {
      // 如果没有 region 文件，用空数据
      this.setData({
        provinces: ['北京市', '上海市', '广东省', '浙江省', '江苏省'],
        regionData: [
          { name: '北京市', cities: [{ name: '北京市', districts: ['东城区', '西城区', '朝阳区', '海淀区'] }] },
          { name: '上海市', cities: [{ name: '上海市', districts: ['黄浦区', '徐汇区', '浦东新区'] }] },
          { name: '广东省', cities: [{ name: '广州市', districts: ['天河区', '越秀区', '海珠区'] }, { name: '深圳市', districts: ['南山区', '福田区', '罗湖区'] }] },
          { name: '浙江省', cities: [{ name: '杭州市', districts: ['西湖区', '滨江区', '余杭区'] }, { name: '宁波市', districts: ['海曙区', '鄞州区'] }] },
          { name: '江苏省', cities: [{ name: '南京市', districts: ['鼓楼区', '玄武区', '建邺区'] }, { name: '苏州市', districts: ['姑苏区', '吴中区', '工业园区'] }] },
        ],
      });
    }
  },

  // 省份选择
  onProvinceChange(e) {
    const idx = e.detail.value;
    const regionData = this.data.regionData;
    if (!regionData || !regionData[idx]) return;
    const province = regionData[idx];
    const cities = (province.cities || []).map(c => c.name);
    this.setData({
      provinceIndex: idx,
      province: province.name,
      cities,
      cityIndex: 0,
      city: cities[0] || '',
      districts: [],
      districtIndex: 0,
      district: '',
    });
  },

  // 城市选择
  onCityChange(e) {
    const idx = e.detail.value;
    const regionData = this.data.regionData;
    const province = regionData[this.data.provinceIndex];
    if (!province || !province.cities || !province.cities[idx]) return;
    const city = province.cities[idx];
    const districts = city.districts || [];
    this.setData({
      cityIndex: idx,
      city: city.name,
      districts,
      districtIndex: 0,
      district: districts[0] || '',
    });
  },

  // 区选择
  onDistrictChange(e) {
    const idx = e.detail.value;
    this.setData({
      districtIndex: idx,
      district: this.data.districts[idx] || '',
    });
  },

  // 表单输入
  onNameInput(e) { this.setData({ name: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onProvinceInput(e) { this.setData({ province: e.detail.value }); },
  onCityInput(e) { this.setData({ city: e.detail.value }); },
  onDistrictInput(e) { this.setData({ district: e.detail.value }); },
  onDetailInput(e) { this.setData({ detailAddress: e.detail.value }); },
  onDefaultChange(e) { this.setData({ isDefault: e.detail.value }); },

  // 保存
  onSave() {
    const { name, phone, province, city, district, detailAddress, isDefault } = this.data;

    if (!name) { showToast('请输入收货人姓名'); return; }
    if (!phone) { showToast('请输入手机号'); return; }
    if (!/^1\d{10}$/.test(phone)) { showToast('请输入正确的手机号'); return; }
    if (!province) { showToast('请输入省份'); return; }
    if (!detailAddress) { showToast('请输入详细地址'); return; }

    const data = { receiverName: name, receiverPhone: phone, province, city, district, detailAddress, isDefault: isDefault ? 1 : 0 };

    wx.showLoading({ title: '保存中...' });

    const request = this.data.isEdit
      ? api.put('/addresses/' + this.data.id, data)
      : api.post('/addresses', data);

    request.then(() => {
      wx.hideLoading();
      showSuccess('保存成功');
      wx.navigateBack();
    }).catch(err => {
      wx.hideLoading();
      showToast(err.message || '保存失败');
    });
  },
});
