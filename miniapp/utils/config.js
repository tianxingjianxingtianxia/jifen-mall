// API基础地址，开发环境连本地后端
const API_BASE_URL = 'http://localhost:8080/api'

// 订单状态
const ORDER_STATUS = {
  0: '待发货',
  1: '已发货', 
  2: '已完成',
  3: '已取消'
}

// 积分类型
const POINT_TYPE = {
  1: '获得',
  2: '消耗'
}

// 商品状态
const PRODUCT_STATUS = {
  0: '下架',
  1: '上架'
}

module.exports = {
  API_BASE_URL,
  ORDER_STATUS,
  POINT_TYPE,
  PRODUCT_STATUS
}
