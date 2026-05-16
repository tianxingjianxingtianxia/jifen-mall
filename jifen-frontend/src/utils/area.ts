/**
 * 省市区三级联动数据
 * 仅包含示例数据，覆盖测试所需城市
 */

export interface AreaItem {
  value: string
  label: string
  children?: AreaItem[]
}

export const areaData: AreaItem[] = [
  {
    value: '北京市',
    label: '北京市',
    children: [
      {
        value: '北京市',
        label: '北京市',
        children: [
          { value: '东城区', label: '东城区' },
          { value: '西城区', label: '西城区' },
          { value: '朝阳区', label: '朝阳区' },
          { value: '海淀区', label: '海淀区' },
          { value: '丰台区', label: '丰台区' },
          { value: '通州区', label: '通州区' },
          { value: '大兴区', label: '大兴区' }
        ]
      }
    ]
  },
  {
    value: '上海市',
    label: '上海市',
    children: [
      {
        value: '上海市',
        label: '上海市',
        children: [
          { value: '黄浦区', label: '黄浦区' },
          { value: '徐汇区', label: '徐汇区' },
          { value: '长宁区', label: '长宁区' },
          { value: '静安区', label: '静安区' },
          { value: '浦东新区', label: '浦东新区' },
          { value: '闵行区', label: '闵行区' }
        ]
      }
    ]
  },
  {
    value: '广东省',
    label: '广东省',
    children: [
      {
        value: '广州市',
        label: '广州市',
        children: [
          { value: '天河区', label: '天河区' },
          { value: '越秀区', label: '越秀区' },
          { value: '海珠区', label: '海珠区' },
          { value: '白云区', label: '白云区' },
          { value: '番禺区', label: '番禺区' },
          { value: '南沙区', label: '南沙区' }
        ]
      },
      {
        value: '深圳市',
        label: '深圳市',
        children: [
          { value: '南山区', label: '南山区' },
          { value: '福田区', label: '福田区' },
          { value: '罗湖区', label: '罗湖区' },
          { value: '宝安区', label: '宝安区' },
          { value: '龙岗区', label: '龙岗区' },
          { value: '龙华区', label: '龙华区' }
        ]
      },
      {
        value: '珠海市',
        label: '珠海市',
        children: [
          { value: '香洲区', label: '香洲区' },
          { value: '金湾区', label: '金湾区' }
        ]
      }
    ]
  },
  {
    value: '湖南省',
    label: '湖南省',
    children: [
      {
        value: '长沙市',
        label: '长沙市',
        children: [
          { value: '岳麓区', label: '岳麓区' },
          { value: '芙蓉区', label: '芙蓉区' },
          { value: '天心区', label: '天心区' },
          { value: '开福区', label: '开福区' },
          { value: '雨花区', label: '雨花区' }
        ]
      },
      {
        value: '株洲市',
        label: '株洲市',
        children: [
          { value: '天元区', label: '天元区' },
          { value: '芦淞区', label: '芦淞区' }
        ]
      }
    ]
  },
  {
    value: '浙江省',
    label: '浙江省',
    children: [
      {
        value: '杭州市',
        label: '杭州市',
        children: [
          { value: '西湖区', label: '西湖区' },
          { value: '上城区', label: '上城区' },
          { value: '拱墅区', label: '拱墅区' },
          { value: '滨江区', label: '滨江区' },
          { value: '余杭区', label: '余杭区' }
        ]
      },
      {
        value: '宁波市',
        label: '宁波市',
        children: [
          { value: '海曙区', label: '海曙区' },
          { value: '鄞州区', label: '鄞州区' }
        ]
      }
    ]
  },
  {
    value: '江苏省',
    label: '江苏省',
    children: [
      {
        value: '南京市',
        label: '南京市',
        children: [
          { value: '玄武区', label: '玄武区' },
          { value: '秦淮区', label: '秦淮区' },
          { value: '建邺区', label: '建邺区' }
        ]
      }
    ]
  },
  {
    value: '四川省',
    label: '四川省',
    children: [
      {
        value: '成都市',
        label: '成都市',
        children: [
          { value: '锦江区', label: '锦江区' },
          { value: '青羊区', label: '青羊区' },
          { value: '武侯区', label: '武侯区' },
          { value: '高新区', label: '高新区' }
        ]
      }
    ]
  }
]

/** 获取省份列表 */
export function getProvinces(): { value: string; label: string }[] {
  return areaData.map(p => ({ value: p.value, label: p.label }))
}

/** 根据省份获取城市列表 */
export function getCities(province: string): { value: string; label: string }[] {
  const p = areaData.find(p => p.value === province)
  if (!p || !p.children) return []
  return p.children.map(c => ({ value: c.value, label: c.label }))
}

/** 根据省份和城市获取区县列表 */
export function getDistricts(province: string, city: string): { value: string; label: string }[] {
  const p = areaData.find(p => p.value === province)
  if (!p || !p.children) return []
  const c = p.children.find(c => c.value === city)
  if (!c || !c.children) return []
  return c.children.map(d => ({ value: d.value, label: d.label }))
}
