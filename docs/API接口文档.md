# 积分商城 API 接口文档

> 版本 v1.0 | 2026-05-20 | 39 个接口

---

## 通用说明

### 认证机制

- **JWT Bearer Token**：`Authorization: Bearer <token>`
- **公开接口**（无需认证）：注册、登录、商品浏览
- **管理员接口**（JWT + isAdmin）：所有 `/admin/*` 路径
- Token 获取：登录成功后返回 `token` 字段

### 通用响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": <T> }

// 分页
{ "code": 200, "message": "success", "data": { "records": [], "total": 100, "pageNum": 1, "pageSize": 10, "pages": 10 } }

// 失败
{ "code": 400/401/403/500, "message": "错误描述", "data": null }
```

### 错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录/token失效 |
| 403 | 无权限 |
| 500 | 服务端错误 |

---

## 1. 认证模块 `/auth`

### 1.1 用户注册
```
POST /api/auth/register  [公开]
```
**请求体：**
| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| username | String | ✅ | 用户名 |
| password | String | ✅ | 密码 |
| nickname | String | - | 昵称 |
| phone | String | - | 手机号 |

**响应：** `Result<LoginResponse>` — 注册成功直接返回token

---

### 1.2 用户登录
```
POST /api/auth/login  [公开]
```
| 字段 | 类型 | 必填 |
|------|------|:---:|
| username | String | ✅ |
| password | String | ✅ |

**响应：** `Result<LoginResponse>`

**LoginResponse：**
| 字段 | 类型 | 说明 |
|------|------|------|
| token | String | JWT令牌 |
| userId | Long | 用户ID |
| username | String | 用户名 |
| nickname | String | 昵称 |
| avatar | String | 头像 |
| points | Integer | 当前积分 |

---

### 1.3 管理员登录
```
POST /api/auth/admin/login  [公开]
```
| 字段 | 类型 | 必填 |
|------|------|:---:|
| username | String | ✅ |
| password | String | ✅ |

**响应：** `Result<AdminLoginResponse>`
| 字段 | 类型 |
|------|------|
| token | String |
| userId | Long |
| username | String |
| nickname | String |

---

### 1.4 获取当前用户信息
```
GET /api/auth/userinfo  [JWT]
```
无参数。从JWT解析userId。

**响应：** `Result<LoginResponse>`

---

### 1.5 微信小程序登录
```
POST /api/auth/wx-login  [公开]
```
| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| code | String | ✅ | wx.login返回的code |

**响应：** `Result<LoginResponse>`

---

## 2. 商品模块 `/products`

### 2.1 商品列表
```
GET /api/products  [公开]
```
| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|:---:|------|------|
| keyword | String | - | - | 搜索关键词 |
| sortBy | String | - | - | points_asc / points_desc |
| pageNum | int | - | 1 | 页码 |
| pageSize | int | - | 12 | 每页条数 |

**响应：** `Result<PageResult<ProductListVO>>`

**ProductListVO：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品ID |
| name | String | 商品名称 |
| coverImage | String | 封面图URL |
| pointsRequired | Integer | 所需积分 |
| stock | Integer | 库存 |
| saleCount | Integer | 兑换次数 |
| stockStatus | String | 库存状态 |

---

### 2.2 商品详情
```
GET /api/products/{id}  [公开]
```
**响应：** `Result<ProductVO>`（不存在返回404）

**ProductVO：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | |
| name | String | |
| description | String | 商品描述 |
| coverImage | String | |
| pointsRequired | Integer | |
| stock | Integer | |
| status | Integer | 0=下架 1=上架 |
| sortOrder | Integer | 排序 |
| saleCount | Integer | |
| images | List\<String\> | 详情图片URLs |
| stockStatus | String | |

---

## 3. 积分模块 `/points`

### 3.1 签到
```
POST /api/points/sign-in  [JWT]
```
无参数。每日限1次。

**响应：** `Result<SignInResponse>`
| 字段 | 类型 |
|------|------|
| todaySigned | Boolean |
| points | Integer | 本次获得积分 |
| totalPoints | Integer | 签到后总积分 |

---

### 3.2 查询今日是否签到
```
GET /api/points/today-sign  [JWT]
```
**响应：** `Result<Boolean>`（true=已签到）

---

### 3.3 积分余额
```
GET /api/points/balance  [JWT]
```
**响应：** `Result<BalanceResponse>`
| 字段 | 类型 |
|------|------|
| points | Integer | 当前可用积分 |
| totalEarned | Integer | 累计获得 |
| totalSpent | Integer | 累计消耗 |

---

### 3.4 积分明细
```
GET /api/points/records  [JWT]
```
| 参数 | 类型 | 默认 |
|------|------|------|
| pageNum | int | 1 |
| pageSize | int | 10 |

**响应：** `Result<PageResult<PointRecordVO>>`

**PointRecordVO：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | |
| points | Integer | 变动积分 |
| type | Integer | 1=获得 2=消耗 |
| source | String | 来源(SIGN_IN/EXCHANGE/CANCEL/MANUAL_ADJUST) |
| remark | String | 备注 |
| createTime | String | |
| expireTime | String | 过期时间 |

---

## 4. 地址模块 `/addresses`

> 全部需要 JWT 认证

### 4.1 地址列表
```
GET /api/addresses  [JWT]
```
**响应：** `Result<List<AddressVO>>`

### 4.2 新增地址
```
POST /api/addresses  [JWT]
```
| 字段 | 类型 | 必填 | 校验 |
|------|------|:---:|------|
| receiverName | String | ✅ | @NotBlank "收货人不能为空" |
| receiverPhone | String | ✅ | @NotBlank "手机号不能为空" |
| province | String | ✅ | @NotBlank "省份不能为空" |
| city | String | ✅ | @NotBlank "城市不能为空" |
| district | String | ✅ | @NotBlank "区县不能为空" |
| detailAddress | String | ✅ | @NotBlank "详细地址不能为空" |
| isDefault | Integer | - | 是否默认 |

### 4.3 编辑地址
```
PUT /api/addresses/{id}  [JWT]
```
参数同新增。

### 4.4 删除地址
```
DELETE /api/addresses/{id}  [JWT]
```

### 4.5 设为默认地址
```
PUT /api/addresses/{id}/default  [JWT]
```

**AddressVO 响应字段：**
`id`(Long), `userId`(Long), `receiverName`(String), `receiverPhone`(String), `province`(String), `city`(String), `district`(String), `detailAddress`(String), `isDefault`(Integer)

---

## 5. 订单模块 `/orders`

> 全部需要 JWT 认证

### 5.1 创建兑换订单
```
POST /api/orders  [JWT]
```
| 字段 | 类型 | 必填 | 校验 |
|------|------|:---:|------|
| productId | Long | ✅ | @NotNull "商品ID不能为空" |
| addressId | Long | ✅ | @NotNull "收货地址ID不能为空" |

**业务规则：**
- 积分不足 → 400
- 库存不足 → 400
- 30天内重复兑换 → 400
- 并发防超卖（Redis分布式锁）

**响应：** `Result<OrderVO>`

---

### 5.2 订单列表
```
GET /api/orders  [JWT]
```
| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| status | Integer | - | 0待发货 1已发货 2已完成 3已取消 |
| pageNum | int | 1 | |
| pageSize | int | 10 | |

---

### 5.3 订单详情
```
GET /api/orders/{id}  [JWT]
```

### 5.4 取消订单
```
POST /api/orders/{id}/cancel  [JWT]
```
条件：订单状态=待发货 且 创建15分钟内

### 5.5 确认收货
```
POST /api/orders/{id}/confirm  [JWT]
```
条件：订单状态=已发货

**OrderVO：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单ID |
| orderNo | String | 订单号 |
| userId | Long | |
| productId | Long | |
| productName | String | |
| productImage | String | |
| pointsSpent | Integer | 消耗积分 |
| receiverName | String | |
| receiverPhone | String | |
| receiverAddress | String | |
| status | Integer | **0=待发货 1=已发货 2=已完成 3=已取消** |
| statusText | String | 状态文本 |
| trackingNo | String | 物流单号 |
| createTime | String | |
| expireTime | String | 超时取消时间（创建+15分钟） |

---

## 6. 文件上传

### 6.1 上传图片
```
POST /api/admin/upload  [JWT]
```
multipart/form-data，字段 `file`。

限制：最大5MB，支持 jpg/jpeg/png/gif。

**响应：** `Result<String>` — 返回图片URL

---

## 7. 管理后台 `/admin`

> 全部需要 JWT + 管理员角色

### 7.1 数据看板
```
GET /api/admin/dashboard  [JWT+Admin]
```
**响应：** `Result<DashboardVO>`
| 字段 | 类型 |
|------|------|
| totalUsers | long |
| totalProducts | long |
| totalOrders | long |
| todaySignIns | long |
| pendingOrders | long |
| totalPointsEarned | long |
| totalPointsSpent | long |
| exchangeTrend | Map | 兑换趋势数据 |

---

### 7.2 商品管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/products` | 商品列表（keyword/status/pageNum/pageSize） |
| POST | `/admin/products` | 新增商品 |
| PUT | `/admin/products/{id}` | 编辑商品 |
| PUT | `/admin/products/{id}/status` | 上下架切换 |
| DELETE | `/admin/products/{id}` | 删除商品 |
| GET | `/admin/products/export` | 导出CSV |

**ProductFormRequest（新增/编辑共用）：**
| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 商品名称 |
| description | String | 描述 |
| coverImage | String | 封面图URL |
| pointsRequired | Integer | 所需积分 |
| stock | Integer | 库存 |
| sortOrder | Integer | 排序 |
| images | List\<String\> | 详情图片列表 |

---

### 7.3 订单管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/orders` | 订单列表（status/orderNo/pageNum/pageSize） |
| PUT | `/admin/orders/{id}/ship` | 发货（trackingNo） |
| GET | `/admin/orders/export` | 导出CSV |

**ShipRequest：** `trackingNo`(String)

---

### 7.4 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/users` | 用户列表（keyword/pageNum/pageSize） |
| PUT | `/admin/users/{userId}/points` | 手动调整积分 |
| PUT | `/admin/users/{userId}/status` | 启用/禁用 |
| GET | `/admin/users/export` | 导出CSV |

**积分调整请求体：** `{ "points": int, "source": "MANUAL_ADJUST", "remark": "管理员手动调整" }`

---

### 7.5 系统配置
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/config` | 获取配置 |
| PUT | `/admin/config` | 更新配置（Map\<String,String\>） |

### 7.6 积分过期清理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/expired-points` | 查看将过期积分 |
| POST | `/admin/points/clean-expired` | 执行过期积分清理 |

---

## 接口汇总

| 模块 | 接口数 | 认证 |
|------|:---:|------|
| 认证 | 5 | 3公开 / 2 JWT |
| 商品 | 2 | 全公开 |
| 积分 | 5 | 全部JWT |
| 地址 | 5 | 全部JWT |
| 订单 | 5 | 全部JWT |
| 上传 | 1 | JWT |
| 管理后台 | 16 | JWT+Admin |
| **合计** | **39** | 6公开 / 33 JWT |
