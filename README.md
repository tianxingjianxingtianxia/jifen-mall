# jifen-mall 积分商城系统

## 技术栈
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis
- 前端：Vue 3 + Element Plus + TypeScript
- 部署：Docker / docker-compose

## 快速启动

### 1. 数据库
```bash
mysql -h 192.168.1.49 -P 13306 -u user_test -p wj_jifen --default-character-set=utf8 < jifen-backend/init.sql
```

### 2. 后端
```bash
cd jifen-backend
mvn spring-boot:run
```
服务启动在 http://localhost:8080/api

### 3. 前端
```bash
cd jifen-frontend
npm install
npm run dev
```
页面访问 http://localhost:3000

## 默认账号
- 管理员：admin / admin123
- 普通用户：通过注册页面注册

## API 概览

| 模块 | 路径 | 权限 |
|:---|:---|:---:|
| 认证 | `/api/auth/*` | 注册/登录公开，用户信息需登录 |
| 积分中心 | `/api/points/*` | 需登录 |
| 商品中心 | `/api/products/*` | 公开（GET） |
| 地址管理 | `/api/addresses/*` | 需登录 |
| 兑换订单 | `/api/orders/*` | 需登录 |
| 管理后台 | `/api/admin/*` | 需管理员权限 |

## 定时任务
- 订单超时自动取消：每分钟扫描，15 分钟未处理自动取消
- 积分过期清理：每年 12 月 31 日 23:59:59 执行

## 并发控制
- 防超卖：MySQL 乐观锁 `UPDATE ... WHERE stock > 0`
- 重复兑换限制：同一用户同商品 30 天内限兑 1 次
- 取消时限：订单创建后 15 分钟内可主动取消

## 项目结构
```
jifen-mall/
├── jifen-backend/          # Spring Boot 后端
│   ├── src/main/java/com/jifen/
│   │   ├── auth/           # 认证模块
│   │   ├── common/         # 公共组件
│   │   ├── config/         # 配置
│   │   ├── modules/        # 业务模块
│   │   │   ├── points/     # 积分中心
│   │   │   ├── product/    # 商品中心
│   │   │   ├── address/    # 地址管理
│   │   │   ├── order/      # 兑换订单
│   │   │   └── admin/      # 管理后台
│   │   └── task/           # 定时任务
│   ├── src/test/           # 65 个测试
│   ├── init.sql            # 数据库初始化
│   └── Dockerfile
├── jifen-frontend/         # Vue3 前端
│   └── src/
│       ├── api/            # API 调用
│       ├── views/          # 页面
│       │   └── admin/      # 管理后台页面
│       ├── router/         # 路由
│       └── stores/         # 状态管理
└── docker-compose.yml      # 容器编排
```

## 测试
```bash
cd jifen-backend && mvn test
# 65 个测试全部通过
```
