# 积分商城 API 接口文档

**基础路径**: `/api`

**通用响应格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**认证方式**: Bearer Token (除登录/注册外均需在 Header 中携带 `Authorization: Bearer <token>`)

**枚举值汇总**:
| 字段 | 枚举值 |
|:---|:---|
| 订单状态 `status` | `0`=待发货 `1`=已发货 `2`=已完成 `3`=已取消 |
| 商品状态 `status` | `0`=下架 `1`=上架 |
| 积分记录类型 `type` | `1`=获得 `2`=消耗 |
| 积分记录来源 `source` | `SIGN_IN`=签到 `EXCHANGE`=兑换 `ORDER_CANCEL`=取消订单 `EXPIRE`=过期清零 |
| 地址是否默认 `isDefault` | `0`=非默认 `1`=默认 |
| 库存状态 `stockStatus` | `"已售罄"` `"库存紧张"`(≤5) `"有货"` |

---

## 1. 认证模块 `/auth`

### 1.1 用户注册

- **Method**: `POST`
- **Path**: `/auth/register`
- **认证**: 无需认证

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| nickname | String | 是 | 昵称 |

**示例请求**:
```json
{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三"
}
```

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| token | String | JWT Token |
| userId | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| points | Integer | 积分余额 |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "points": 0
  }
}
```

---

### 1.2 用户登录

- **Method**: `POST`
- **Path**: `/auth/login`
- **认证**: 无需认证

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**示例请求**:
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应体 (data 字段)**: 同注册响应

---

### 1.3 管理员登录

- **Method**: `POST`
- **Path**: `/auth/admin/login`
- **认证**: 无需认证

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| username | String | 是 | 管理员用户名 |
| password | String | 是 | 管理员密码 |

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| token | String | JWT Token |
| userId | Long | 管理员用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |

---

### 1.4 获取用户信息

- **Method**: `GET`
- **Path**: `/auth/userinfo`
- **认证**: 需要 Bearer Token

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| token | String | JWT Token |
| userId | Long | 用户 ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| points | Integer | 积分余额 |

---

## 2. 积分模块 `/points`

### 2.1 每日签到

- **Method**: `POST`
- **Path**: `/points/sign-in`
- **认证**: 需要 Bearer Token

**请求体**: 无

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| todaySigned | Boolean | `true`=签到成功 |
| points | Integer | 本次签到获得的积分数 |
| totalPoints | Integer | 签到后总积分 |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todaySigned": true,
    "points": 10,
    "totalPoints": 110
  }
}
```

**错误码**:
| 状态码 | 说明 |
|:---:|:---|
| 500 | 今日已签到 |

---

### 2.2 查询今日是否已签到

- **Method**: `GET`
- **Path**: `/points/today-sign`
- **认证**: 需要 Bearer Token

**响应体 (data 字段)**: Boolean

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

---

### 2.3 查询积分余额

- **Method**: `GET`
- **Path**: `/points/balance`
- **认证**: 需要 Bearer Token

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| points | Integer | 当前可用积分 |
| totalEarned | Integer | 累计获得积分 |
| totalSpent | Integer | 累计消耗积分 |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "points": 100,
    "totalEarned": 200,
    "totalSpent": 100
  }
}
```

---

### 2.4 积分明细（分页）

- **Method**: `GET`
- **Path**: `/points/records`
- **认证**: 需要 Bearer Token

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|:---|:---:|:---:|:---:|:---|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| records | Array | 积分记录列表 |
| total | Long | 总记录数 |
| pageNum | Long | 当前页码 |
| pageSize | Long | 每页条数 |
| pages | Long | 总页数 |

**records 中每个元素**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| id | Long | 记录 ID |
| points | Integer | 积分数值（正数为获得，负数为消耗） |
| type | Integer | 类型: `1`=获得 `2`=消耗 |
| source | String | 来源: `SIGN_IN` `EXCHANGE` `ORDER_CANCEL` `EXPIRE` |
| remark | String | 备注说明 |
| createTime | String (ISO DateTime) | 创建时间 |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "points": 10,
        "type": 1,
        "source": "SIGN_IN",
        "remark": "每日签到",
        "createTime": "2026-05-16T10:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

---

## 3. 商品模块 `/products`

### 3.1 商品列表

- **Method**: `GET`
- **Path**: `/products`
- **认证**: 需要 Bearer Token（游客可能的默认行为待确认）

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|:---|:---:|:---:|:---:|:---|
| keyword | String | 否 | - | 按名称模糊搜索 |
| sortBy | String | 否 | - | 排序: `points_asc`=积分升序 `points_desc`=积分降序 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 12 | 每页条数 |

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| records | Array | 商品列表 |
| total | Long | 总记录数 |
| pageNum | Long | 当前页码 |
| pageSize | Long | 每页条数 |
| pages | Long | 总页数 |

**records 中每个元素**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| id | Long | 商品 ID |
| name | String | 商品名称 |
| coverImage | String | 封面图 URL |
| pointsRequired | Integer | 所需积分数 |
| stock | Integer | 库存数量 |
| saleCount | Integer | 已兑换数量 |
| stockStatus | String | 库存状态: `"有货"` `"库存紧张"`(≤5) `"已售罄"`(=0) |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "定制马克杯",
        "coverImage": "https://cdn.example.com/mug.jpg",
        "pointsRequired": 50,
        "stock": 20,
        "saleCount": 5,
        "stockStatus": "有货"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 12,
    "pages": 1
  }
}
```

---

### 3.2 商品详情

- **Method**: `GET`
- **Path**: `/products/{id}`
- **认证**: 需要 Bearer Token（同商品列表）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 商品 ID |

**响应体 (data 字段)**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| id | Long | 商品 ID |
| name | String | 商品名称 |
| description | String | 商品描述 |
| coverImage | String | 封面图 URL |
| pointsRequired | Integer | 所需积分数 |
| stock | Integer | 库存数量 |
| status | Integer | 商品状态: `0`=下架 `1`=上架 |
| sortOrder | Integer | 排序权重 |
| saleCount | Integer | 已兑换数量 |
| images | Array of String | 商品轮播图 URL 列表 |
| stockStatus | String | 库存状态 |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "定制马克杯",
    "description": "精美定制马克杯，容量350ml",
    "coverImage": "https://cdn.example.com/mug.jpg",
    "pointsRequired": 50,
    "stock": 20,
    "status": 1,
    "sortOrder": 0,
    "saleCount": 5,
    "images": [
      "https://cdn.example.com/mug-1.jpg",
      "https://cdn.example.com/mug-2.jpg"
    ],
    "stockStatus": "有货"
  }
}
```

---

## 4. 地址管理模块 `/addresses`

### 4.1 地址列表

- **Method**: `GET`
- **Path**: `/addresses`
- **认证**: 需要 Bearer Token

**响应体 (data 字段)**: Array of AddressVO

**AddressVO 字段**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| id | Long | 地址 ID |
| userId | Long | 用户 ID |
| receiverName | String | 收货人姓名 |
| receiverPhone | String | 收货人手机号 |
| province | String | 省份 |
| city | String | 城市 |
| district | String | 区县 |
| detailAddress | String | 详细地址 |
| isDefault | Integer | 是否默认: `0`=非默认 `1`=默认 |

---

### 4.2 新增地址

- **Method**: `POST`
- **Path**: `/addresses`
- **认证**: 需要 Bearer Token

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| receiverName | String | 是 | 收货人姓名 |
| receiverPhone | String | 是 | 收货人手机号 |
| province | String | 是 | 省份 |
| city | String | 是 | 城市 |
| district | String | 是 | 区县 |
| detailAddress | String | 是 | 详细地址 |
| isDefault | Integer | 否 | 是否默认: `0`=非默认 `1`=默认 |

**响应体 (data 字段)**: AddressVO

---

### 4.3 更新地址

- **Method**: `PUT`
- **Path**: `/addresses/{id}`
- **认证**: 需要 Bearer Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 地址 ID |

**请求体**: 同新增地址

**响应体 (data 字段)**: AddressVO

---

### 4.4 删除地址

- **Method**: `DELETE`
- **Path**: `/addresses/{id}`
- **认证**: 需要 Bearer Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 地址 ID |

**响应体**: 无 data (code=200 表示成功)

---

### 4.5 设为默认地址

- **Method**: `PUT`
- **Path**: `/addresses/{id}/default`
- **认证**: 需要 Bearer Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 地址 ID |

**响应体**: 无 data (code=200 表示成功)

---

## 5. 订单模块 `/orders`

### 5.1 创建兑换订单

- **Method**: `POST`
- **Path**: `/orders`
- **认证**: 需要 Bearer Token

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| productId | Long | 是 | 商品 ID |
| addressId | Long | 是 | 收货地址 ID |

**示例请求**:
```json
{
  "productId": 1,
  "addressId": 1
}
```

**响应体 (data 字段)**: OrderVO

**OrderVO 字段**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| id | Long | 订单 ID |
| orderNo | String | 订单号 (JF + yyyyMMddHHmmss + 4位随机数) |
| userId | Long | 用户 ID |
| productId | Long | 商品 ID |
| productName | String | 商品名称 |
| productImage | String | 商品图片 URL |
| pointsSpent | Integer | 消耗积分数 |
| addressId | Long | 收货地址 ID |
| receiverName | String | 收货人 |
| receiverPhone | String | 收货人手机 |
| receiverAddress | String | 完整收货地址 |
| status | Integer | 订单状态: `0`=待发货 `1`=已发货 `2`=已完成 `3`=已取消 |
| statusText | String | 状态中文描述 |
| trackingNo | String | 物流单号 |
| cancelReason | String | 取消原因 |
| cancelTime | String (ISO DateTime) | 取消时间 |
| paidAt | String (ISO DateTime) | 兑换时间 |
| shippedAt | String (ISO DateTime) | 发货时间 |
| confirmedAt | String (ISO DateTime) | 确认收货时间 |
| expireTime | String (ISO DateTime) | 自动取消截止时间 |
| createTime | String (ISO DateTime) | 创建时间 |

**错误码**:
| 状态码 | 说明 |
|:---:|:---|
| 500 | 商品不存在 / 商品已下架 / 商品库存不足 |
| 500 | 同一商品30天内只能兑换1次 |
| 500 | 积分不足 |
| 500 | 收货地址不存在 |

---

### 5.2 订单列表

- **Method**: `GET`
- **Path**: `/orders`
- **认证**: 需要 Bearer Token

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|:---|:---:|:---:|:---:|:---|
| status | Integer | 否 | - | 筛选订单状态: `0`=待发货 `1`=已发货 `2`=已完成 `3`=已取消 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**响应体 (data 字段)**: 分页结果，records 为 OrderVO 数组

---

### 5.3 订单详情

- **Method**: `GET`
- **Path**: `/orders/{id}`
- **认证**: 需要 Bearer Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 订单 ID |

**响应体 (data 字段)**: OrderVO

---

### 5.4 取消订单

- **Method**: `POST`
- **Path**: `/orders/{id}/cancel`
- **认证**: 需要 Bearer Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 订单 ID |

**限制条件**:
- 仅订单状态为 `0`(待发货) 时可取消
- 兑换后超过 15 分钟不可取消
- 取消后积分退回、库存恢复

**响应体**: 无 data (code=200 表示成功)

---

### 5.5 确认收货

- **Method**: `POST`
- **Path**: `/orders/{id}/confirm`
- **认证**: 需要 Bearer Token

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 订单 ID |

**限制条件**:
- 仅订单状态为 `1`(已发货) 时可确认收货

**响应体**: 无 data (code=200 表示成功)

---

## 6. 管理后台 `/admin`

> 所有管理后台接口均需管理员权限，请求头需携带管理员 Bearer Token，否则返回 `403 无管理员权限`

### 6.1 商品列表（管理员）

- **Method**: `GET`
- **Path**: `/admin/products`
- **认证**: Bearer Token + 管理员权限

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|:---|:---:|:---:|:---:|:---|
| keyword | String | 否 | - | 按名称搜索 |
| status | Integer | 否 | - | 商品状态: `0`=下架 `1`=上架 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**响应体 (data 字段)**: 分页结果，records 包含完整商品字段及 createTime/updateTime

---

### 6.2 新增商品

- **Method**: `POST`
- **Path**: `/admin/products`
- **认证**: Bearer Token + 管理员权限

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| name | String | 否 | 商品名称 |
| description | String | 否 | 商品描述 |
| coverImage | String | 否 | 封面图 URL |
| pointsRequired | Integer | 否 | 所需积分 |
| stock | Integer | 否 | 库存数量 |
| sortOrder | Integer | 否 | 排序权重（默认为 0） |

**响应体 (data 字段)**: 新建商品的 ID (Long)

---

### 6.3 编辑商品

- **Method**: `PUT`
- **Path**: `/admin/products/{id}`
- **认证**: Bearer Token + 管理员权限

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 商品 ID |

**请求体**: 同新增商品

**响应体 (data 字段)**: 商品 ID (Long)

---

### 6.4 上下架商品

- **Method**: `PUT`
- **Path**: `/admin/products/{id}/status`
- **认证**: Bearer Token + 管理员权限

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 商品 ID |

**说明**: 切换商品状态，上架⇄下架

**响应体**: 无 data

---

### 6.5 删除商品

- **Method**: `DELETE`
- **Path**: `/admin/products/{id}`
- **认证**: Bearer Token + 管理员权限

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 商品 ID |

**说明**: 逻辑删除 (is_deleted 标记)

**响应体**: 无 data

---

### 6.6 订单列表（管理员）

- **Method**: `GET`
- **Path**: `/admin/orders`
- **认证**: Bearer Token + 管理员权限

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|:---|:---:|:---:|:---:|:---|
| status | Integer | 否 | - | 订单状态: `0`=待发货 `1`=已发货 `2`=已完成 `3`=已取消 |
| orderNo | String | 否 | - | 按订单号搜索 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**响应体 (data 字段)**: 分页结果，records 为 OrderVO 数组

---

### 6.7 订单发货

- **Method**: `PUT`
- **Path**: `/admin/orders/{id}/ship`
- **认证**: Bearer Token + 管理员权限

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| id | Long | 是 | 订单 ID |

**请求体**:
| 字段名 | 类型 | 必填 | 说明 |
|:---|:---:|:---:|:---|
| trackingNo | String | 否 | 物流单号 |

**限制条件**: 仅订单状态为 `0`(待发货) 时可发货

**响应体**: 无 data

---

### 6.8 获取系统配置

- **Method**: `GET`
- **Path**: `/admin/config`
- **认证**: Bearer Token + 管理员权限

**响应体 (data 字段)**: Map<String, String>

**默认配置项**:
| 配置键 | 说明 | 默认值 |
|:---|:---|:---:|
| sign_in_points | 签到奖励积分数 | `10` |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sign_in_points": "10"
  }
}
```

---

### 6.9 更新系统配置

- **Method**: `PUT`
- **Path**: `/admin/config`
- **认证**: Bearer Token + 管理员权限

**请求体**: Map<String, String>

**示例请求**:
```json
{
  "sign_in_points": "20"
}
```

**响应体**: 无 data

---

### 6.10 统计看板

- **Method**: `GET`
- **Path**: `/admin/dashboard`
- **认证**: Bearer Token + 管理员权限

**响应体 (data 字段) - DashboardVO**:
| 字段名 | 类型 | 说明 |
|:---|:---:|:---|
| totalUsers | long | 激活用户总数 |
| totalProducts | long | 上架商品总数 |
| totalOrders | long | 订单总数 |
| todaySignIns | long | 今日签到人数 |
| pendingOrders | long | 待处理订单数（状态=0） |
| totalPointsEarned | long | 累计发放积分 |
| totalPointsSpent | long | 累计消耗积分 |
| exchangeTrend | Map | 兑换趋势数据 |

**示例响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 100,
    "totalProducts": 20,
    "totalOrders": 50,
    "todaySignIns": 15,
    "pendingOrders": 3,
    "totalPointsEarned": 5000,
    "totalPointsSpent": 2000,
    "exchangeTrend": {}
  }
}
```

---

## 7. 通用响应结构

所有接口返回统一格式：

| 字段 | 类型 | 说明 |
|:---|:---:|:---|
| code | int | 状态码: `200`=成功 `404`=资源不存在 `403`=无权限 `500`=业务错误 |
| message | String | 提示信息 |
| data | T | 响应数据，为 null 时表示无返回数据 |

**分页响应通用格式 (PageResult)**:
| 字段 | 类型 | 说明 |
|:---|:---:|:---|
| records | Array | 当前页数据列表 |
| total | long | 总记录数 |
| pageNum | long | 当前页码 |
| pageSize | long | 每页条数 |
| pages | long | 总页数 |
