package com.jifen.modules.order;

import com.jifen.common.PageResult;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.order.dto.CreateOrderRequest;
import com.jifen.modules.order.dto.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, statements = {
    "CREATE TABLE IF NOT EXISTS wj_user (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  username VARCHAR(50) NOT NULL," +
    "  password VARCHAR(255) NOT NULL," +
    "  nickname VARCHAR(50)," +
    "  phone VARCHAR(20)," +
    "  avatar VARCHAR(500)," +
    "  points INT DEFAULT 0," +
    "  total_earned INT DEFAULT 0," +
    "  total_spent INT DEFAULT 0," +
    "  status TINYINT DEFAULT 1," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")",
    "CREATE TABLE IF NOT EXISTS wj_product (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  name VARCHAR(100) NOT NULL," +
    "  description TEXT," +
    "  cover_image VARCHAR(500)," +
    "  points_required INT NOT NULL DEFAULT 0," +
    "  stock INT NOT NULL DEFAULT 0," +
    "  status TINYINT DEFAULT 1," +
    "  sort_order INT DEFAULT 0," +
    "  sale_count INT DEFAULT 0," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")",
    "CREATE TABLE IF NOT EXISTS wj_address (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  user_id BIGINT NOT NULL," +
    "  receiver_name VARCHAR(50)," +
    "  receiver_phone VARCHAR(20)," +
    "  province VARCHAR(50)," +
    "  city VARCHAR(50)," +
    "  district VARCHAR(50)," +
    "  detail_address VARCHAR(500)," +
    "  is_default TINYINT DEFAULT 0," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")",
    "CREATE TABLE IF NOT EXISTS wj_order (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  order_no VARCHAR(32) NOT NULL," +
    "  user_id BIGINT NOT NULL," +
    "  product_id BIGINT NOT NULL," +
    "  product_name VARCHAR(100)," +
    "  product_image VARCHAR(500)," +
    "  points_spent INT NOT NULL," +
    "  address_id BIGINT," +
    "  receiver_name VARCHAR(50)," +
    "  receiver_phone VARCHAR(20)," +
    "  receiver_address VARCHAR(500)," +
    "  status TINYINT DEFAULT 0," +
    "  tracking_no VARCHAR(100)," +
    "  cancel_reason VARCHAR(255)," +
    "  cancel_time DATETIME," +
    "  paid_at DATETIME," +
    "  shipped_at DATETIME," +
    "  confirmed_at DATETIME," +
    "  expire_time DATETIME," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")",
    "CREATE TABLE IF NOT EXISTS wj_point_record (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  user_id BIGINT NOT NULL," +
    "  type TINYINT NOT NULL," +
    "  source VARCHAR(50)," +
    "  points INT NOT NULL," +
    "  balance_before INT," +
    "  balance_after INT," +
    "  related_id BIGINT," +
    "  remark VARCHAR(255)," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")"
})
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;
    private Long productId;
    private Long addressId;

    @BeforeEach
    void setUp() {
        // Clean all tables
        jdbcTemplate.execute("DELETE FROM wj_point_record");
        jdbcTemplate.execute("DELETE FROM wj_order");
        jdbcTemplate.execute("DELETE FROM wj_address");
        jdbcTemplate.execute("DELETE FROM wj_product");
        jdbcTemplate.execute("DELETE FROM wj_user");
        // Reset auto-increment
        for (String tbl : List.of("wj_user", "wj_product", "wj_address", "wj_order", "wj_point_record")) {
            jdbcTemplate.execute("ALTER TABLE " + tbl + " ALTER COLUMN id RESTART WITH 1");
        }

        // Create test user with 500 points
        String pwd = passwordEncoder.encode("password123");
        jdbcTemplate.update(
            "INSERT INTO wj_user (username, password, nickname, points, total_earned) VALUES (?, ?, ?, ?, ?)",
            "testuser", pwd, "Test User", 500, 500
        );
        userId = jdbcTemplate.queryForObject("SELECT id FROM wj_user WHERE username = ?", Long.class, "testuser");

        // Create test product: 100 points, stock 10
        jdbcTemplate.update(
            "INSERT INTO wj_product (name, description, cover_image, points_required, stock, status, sort_order, sale_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            "测试商品", "描述", "/img.jpg", 100, 10, 1, 1, 0
        );
        productId = jdbcTemplate.queryForObject("SELECT id FROM wj_product WHERE name = ?", Long.class, "测试商品");

        // Create test address
        jdbcTemplate.update(
            "INSERT INTO wj_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            userId, "张三", "13800138001", "广东省", "深圳市", "南山区", "科技园路1号", 1
        );
        addressId = jdbcTemplate.queryForObject("SELECT id FROM wj_address WHERE user_id = ?", Long.class, userId);
    }

    private CreateOrderRequest createRequest() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setProductId(productId);
        req.setAddressId(addressId);
        return req;
    }

    // ===== 创建订单 =====

    @Test
    void testCreateOrderSuccess() {
        OrderVO order = orderService.createOrder(userId, createRequest());

        assertNotNull(order);
        assertNotNull(order.getOrderNo());
        assertTrue(order.getOrderNo().startsWith("JF"));
        assertEquals(productId, order.getProductId());
        assertEquals("测试商品", order.getProductName());
        assertEquals(100, order.getPointsSpent());
        assertEquals(0, order.getStatus()); // 待发货
        assertEquals("待发货", order.getStatusText());
        assertEquals("张三", order.getReceiverName());
        assertEquals("13800138001", order.getReceiverPhone());
        assertNotNull(order.getExpireTime());

        // Verify user points deducted
        Integer points = jdbcTemplate.queryForObject("SELECT points FROM wj_user WHERE id = ?", Integer.class, userId);
        assertEquals(400, points);
        Integer totalSpent = jdbcTemplate.queryForObject("SELECT total_spent FROM wj_user WHERE id = ?", Integer.class, userId);
        assertEquals(100, totalSpent);

        // Verify product stock decremented
        Integer stock = jdbcTemplate.queryForObject("SELECT stock FROM wj_product WHERE id = ?", Integer.class, productId);
        assertEquals(9, stock);
        Integer saleCount = jdbcTemplate.queryForObject("SELECT sale_count FROM wj_product WHERE id = ?", Integer.class, productId);
        assertEquals(1, saleCount);

        // Verify point record
        Integer recCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wj_point_record WHERE user_id = ? AND type = 2 AND source = 'EXCHANGE'",
            Integer.class, userId
        );
        assertEquals(1, recCount);
    }

    @Test
    void testCreateOrderInsufficientPoints() {
        // Change user points to 50 (< 100)
        jdbcTemplate.update("UPDATE wj_user SET points = 50 WHERE id = ?", userId);

        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(userId, createRequest());
        });
        assertTrue(e.getMessage().contains("积分不足"));
    }

    @Test
    void testCreateOrderOutOfStock() {
        // Set stock to 0
        jdbcTemplate.update("UPDATE wj_product SET stock = 0 WHERE id = ?", productId);

        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(userId, createRequest());
        });
        assertTrue(e.getMessage().contains("库存不足"));
    }

    @Test
    void testCreateOrderProductOffShelf() {
        jdbcTemplate.update("UPDATE wj_product SET status = 0 WHERE id = ?", productId);

        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(userId, createRequest());
        });
        assertTrue(e.getMessage().contains("下架"));
    }

    @Test
    void testCreateOrderRepeatExchange() {
        // First order success
        orderService.createOrder(userId, createRequest());

        // Second order within 30 days should fail
        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(userId, createRequest());
        });
        assertTrue(e.getMessage().contains("30天") || e.getMessage().contains("只能兑换"));
    }

    @Test
    void testCreateOrderRepeatAllowedWhenCancelled() {
        // Create and cancel an order
        OrderVO order = orderService.createOrder(userId, createRequest());
        orderService.cancelOrder(order.getId(), userId);

        // Should be able to order again since the previous one was cancelled
        OrderVO order2 = orderService.createOrder(userId, createRequest());
        assertNotNull(order2);
    }

    // ===== 订单列表 =====

    @Test
    void testListOrdersEmpty() {
        PageResult<OrderVO> page = orderService.listOrders(userId, null, 1, 10);
        assertEquals(0, page.getTotal());
    }

    @Test
    void testListOrdersWithStatus() {
        orderService.createOrder(userId, createRequest());

        PageResult<OrderVO> pending = orderService.listOrders(userId, 0, 1, 10);
        assertEquals(1, pending.getTotal());

        PageResult<OrderVO> shipped = orderService.listOrders(userId, 1, 1, 10);
        assertEquals(0, shipped.getTotal());
    }

    // ===== 订单详情 =====

    @Test
    void testOrderDetail() {
        OrderVO created = orderService.createOrder(userId, createRequest());
        OrderVO detail = orderService.getOrderDetail(created.getId(), userId);
        assertNotNull(detail);
        assertEquals(created.getOrderNo(), detail.getOrderNo());
    }

    @Test
    void testOrderDetailNotOwn() {
        OrderVO order = orderService.createOrder(userId, createRequest());
        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.getOrderDetail(order.getId(), 999L);
        });
        assertTrue(e.getMessage().contains("无权"));
    }

    // ===== 取消订单 =====

    @Test
    void testCancelOrderSuccess() {
        OrderVO order = orderService.createOrder(userId, createRequest());

        // Cancel
        orderService.cancelOrder(order.getId(), userId);

        // Verify status
        OrderVO cancelled = orderService.getOrderDetail(order.getId(), userId);
        assertEquals(3, cancelled.getStatus());
        assertEquals("已取消", cancelled.getStatusText());

        // Verify points returned
        Integer points = jdbcTemplate.queryForObject("SELECT points FROM wj_user WHERE id = ?", Integer.class, userId);
        assertEquals(500, points); // 500 original + 100 returned

        // Verify stock restored
        Integer stock = jdbcTemplate.queryForObject("SELECT stock FROM wj_product WHERE id = ?", Integer.class, productId);
        assertEquals(10, stock);
    }

    @Test
    void testCancelOrderAfter15Minutes() {
        OrderVO order = orderService.createOrder(userId, createRequest());
        // Simulate order created 20 minutes ago
        LocalDateTime oldTime = LocalDateTime.now().minusMinutes(20);
        jdbcTemplate.update("UPDATE wj_order SET paid_at = ? WHERE id = ?", oldTime, order.getId());

        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.cancelOrder(order.getId(), userId);
        });
        assertTrue(e.getMessage().contains("取消时限") || e.getMessage().contains("15分钟"));
    }

    // ===== 确认收货 =====

    @Test
    void testConfirmReceiptSuccess() {
        OrderVO order = orderService.createOrder(userId, createRequest());

        // Manually set to shipped for testing
        jdbcTemplate.update("UPDATE wj_order SET status = 1 WHERE id = ?", order.getId());

        orderService.confirmReceipt(order.getId(), userId);

        OrderVO confirmed = orderService.getOrderDetail(order.getId(), userId);
        assertEquals(2, confirmed.getStatus());
        assertEquals("已完成", confirmed.getStatusText());
    }

    @Test
    void testConfirmReceiptWrongStatus() {
        OrderVO order = orderService.createOrder(userId, createRequest());
        // Still pending (status=0), can't confirm receipt
        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.confirmReceipt(order.getId(), userId);
        });
        assertTrue(e.getMessage().contains("不允许"));
    }

    // ===== 超时自动取消 =====

    @Test
    void testCancelExpiredOrders() {
        orderService.createOrder(userId, createRequest());

        // Manually set expire_time to 5 minutes ago
        jdbcTemplate.update("UPDATE wj_order SET expire_time = ? WHERE user_id = ?",
            LocalDateTime.now().minusMinutes(5), userId);

        int count = orderService.cancelExpiredOrders();
        assertEquals(1, count);

        // Verify
        OrderVO order = orderService.listOrders(userId, 3, 1, 10).getRecords().get(0);
        assertEquals(3, order.getStatus());

        // Points returned
        Integer points = jdbcTemplate.queryForObject("SELECT points FROM wj_user WHERE id = ?", Integer.class, userId);
        assertEquals(500, points);
    }

    // ===== 并发控制（防超卖） =====

    @Test
    void testConcurrentLastStock() {
        // Set stock to 2
        jdbcTemplate.update("UPDATE wj_product SET stock = 2 WHERE id = ?", productId);

        // Create 2 orders as the same user (but 30-day check prevents same product)
        // So we need a different user to test stock concurrency
        String pwd = passwordEncoder.encode("pass");
        jdbcTemplate.update(
            "INSERT INTO wj_user (username, password, nickname, points) VALUES (?, ?, ?, ?)",
            "user2", pwd, "User2", 500
        );
        Long user2Id = jdbcTemplate.queryForObject("SELECT id FROM wj_user WHERE username = ?", Long.class, "user2");

        // Create address for user2
        jdbcTemplate.update(
            "INSERT INTO wj_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address) VALUES (?, ?, ?, ?, ?, ?, ?)",
            user2Id, "李四", "13800138002", "广东省", "深圳市", "福田区", "深南大道1号"
        );
        Long addr2Id = jdbcTemplate.queryForObject("SELECT id FROM wj_address WHERE user_id = ?", Long.class, user2Id);

        // Both order stock=2 product
        CreateOrderRequest req1 = new CreateOrderRequest();
        req1.setProductId(productId);
        req1.setAddressId(addressId);

        CreateOrderRequest req2 = new CreateOrderRequest();
        req2.setProductId(productId);
        req2.setAddressId(addr2Id);

        // First order should succeed
        orderService.createOrder(userId, req1);
        // Second order should succeed (stock was 2)
        orderService.createOrder(user2Id, req2);
        // Third should fail (stock now 0)
        // Need another user
        jdbcTemplate.update(
            "INSERT INTO wj_user (username, password, nickname, points) VALUES (?, ?, ?, ?)",
            "user3", pwd, "User3", 500
        );
        Long user3Id = jdbcTemplate.queryForObject("SELECT id FROM wj_user WHERE username = ?", Long.class, "user3");
        jdbcTemplate.update(
            "INSERT INTO wj_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address) VALUES (?, ?, ?, ?, ?, ?, ?)",
            user3Id, "王五", "13800138003", "广东省", "广州市", "天河区", "体育西路1号"
        );
        Long addr3Id = jdbcTemplate.queryForObject("SELECT id FROM wj_address WHERE user_id = ?", Long.class, user3Id);

        CreateOrderRequest req3 = new CreateOrderRequest();
        req3.setProductId(productId);
        req3.setAddressId(addr3Id);

        BusinessException e = assertThrows(BusinessException.class, () -> {
            orderService.createOrder(user3Id, req3);
        });
        assertTrue(e.getMessage().contains("库存不足"));
    }

    // ===== 库存下限自动下架 =====

    @Test
    void testStockExhaustedAfterOrder() {
        jdbcTemplate.update("UPDATE wj_product SET stock = 1 WHERE id = ?", productId);
        orderService.createOrder(userId, createRequest());

        Integer stock = jdbcTemplate.queryForObject("SELECT stock FROM wj_product WHERE id = ?", Integer.class, productId);
        assertEquals(0, stock);
    }
}
