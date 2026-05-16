-- ============================================================
-- 积分商城系统 - 数据库初始化脚本
-- 数据库：wj_jifen
-- 说明：导入时请使用 mysql --default-character-set=utf8
-- ============================================================

-- 创建数据库（如尚未创建）
-- CREATE DATABASE IF NOT EXISTS wj_jifen DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE wj_jifen;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username     VARCHAR(50)  NOT NULL COMMENT '用户名',
    password     VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    nickname     VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    phone        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    avatar       VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    points       INT          DEFAULT 0 COMMENT '当前可用积分',
    total_earned INT          DEFAULT 0 COMMENT '累计获得积分',
    total_spent  INT          DEFAULT 0 COMMENT '累计消耗积分',
    status       TINYINT      DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    is_deleted   TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 管理员表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_admin (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
    is_deleted  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员（密码: admin123）
INSERT INTO wj_admin (username, password, nickname, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1);

-- ============================================================
-- 3. 签到记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_sign_in (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id         BIGINT   NOT NULL COMMENT '用户ID',
    sign_date       DATE     NOT NULL COMMENT '签到日期',
    points_awarded  INT      DEFAULT 0 COMMENT '获得积分',
    is_deleted      TINYINT  DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, sign_date),
    KEY idx_user_id (user_id),
    KEY idx_sign_date (sign_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- ============================================================
-- 4. 积分变动明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_point_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    type            TINYINT      NOT NULL COMMENT '类型 1-获得 2-消耗',
    source          VARCHAR(50)  DEFAULT NULL COMMENT '来源（SIGN_IN/EXCHANGE/ORDER_CANCEL/EXPIRE）',
    points          INT          NOT NULL COMMENT '变动积分数值',
    balance_before  INT          DEFAULT NULL COMMENT '变动前余额',
    balance_after   INT          DEFAULT NULL COMMENT '变动后余额',
    related_id      BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id),
    KEY idx_user_time (user_id, create_time),
    KEY idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动明细表';

-- ============================================================
-- 5. 商品表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_product (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name             VARCHAR(100)  NOT NULL COMMENT '商品名称',
    description      TEXT          DEFAULT NULL COMMENT '商品描述',
    cover_image      VARCHAR(500)  DEFAULT NULL COMMENT '封面图URL',
    points_required  INT           NOT NULL DEFAULT 0 COMMENT '所需积分',
    stock            INT           NOT NULL DEFAULT 0 COMMENT '库存',
    status           TINYINT       DEFAULT 1 COMMENT '状态 1-上架 0-下架',
    sort_order       INT           DEFAULT 0 COMMENT '排序权重',
    sale_count       INT           DEFAULT 0 COMMENT '兑换次数',
    is_deleted       TINYINT       DEFAULT 0 COMMENT '逻辑删除',
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_points (points_required),
    KEY idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================================
-- 6. 商品图片表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_product_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    product_id  BIGINT       NOT NULL COMMENT '商品ID',
    image_url   VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

-- ============================================================
-- 7. 收货地址表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_address (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    receiver_name   VARCHAR(50)  DEFAULT NULL COMMENT '收货人',
    receiver_phone  VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    province        VARCHAR(50)  DEFAULT NULL COMMENT '省',
    city            VARCHAR(50)  DEFAULT NULL COMMENT '市',
    district        VARCHAR(50)  DEFAULT NULL COMMENT '区',
    detail_address  VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
    is_default      TINYINT      DEFAULT 0 COMMENT '是否默认 0-否 1-是',
    is_deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================================================
-- 8. 兑换订单表（Sprint 2 使用）
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_no         VARCHAR(32)  NOT NULL COMMENT '订单号',
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    product_id       BIGINT       NOT NULL COMMENT '商品ID',
    product_name     VARCHAR(100) DEFAULT NULL COMMENT '商品名称（快照）',
    product_image    VARCHAR(500) DEFAULT NULL COMMENT '商品图片（快照）',
    points_spent     INT          NOT NULL COMMENT '消耗积分',
    address_id       BIGINT       DEFAULT NULL COMMENT '收货地址ID',
    receiver_name    VARCHAR(50)  DEFAULT NULL COMMENT '收货人（快照）',
    receiver_phone   VARCHAR(20)  DEFAULT NULL COMMENT '收货手机号（快照）',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '收货地址（快照）',
    status           TINYINT      DEFAULT 0 COMMENT '状态 0-待发货 1-已发货 2-已完成 3-已取消',
    tracking_no      VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    cancel_reason    VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    cancel_time      DATETIME     DEFAULT NULL COMMENT '取消时间',
    paid_at          DATETIME     DEFAULT NULL COMMENT '兑换时间',
    shipped_at       DATETIME     DEFAULT NULL COMMENT '发货时间',
    confirmed_at     DATETIME     DEFAULT NULL COMMENT '确认收货时间',
    expire_time      DATETIME     DEFAULT NULL COMMENT '超时取消时间（创建+15min）',
    is_deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status (user_id, status),
    KEY idx_expire (expire_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换订单表';

-- ============================================================
-- 9. 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS wj_sys_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    config_key   VARCHAR(50)  NOT NULL COMMENT '配置键',
    config_value VARCHAR(255) DEFAULT NULL COMMENT '配置值',
    description  VARCHAR(255) DEFAULT NULL COMMENT '说明',
    is_deleted   TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 预置配置数据
INSERT INTO wj_sys_config (config_key, config_value, description) VALUES
('sign_in_points', '10', '签到奖励积分'),
('exchange_ratio', '100:5', '消费兑换比例（100元=5积分，暂未使用）'),
('order_expire_minutes', '15', '订单超时取消分钟数'),
('repeat_exchange_days', '30', '同一商品重复兑换天数限制');
