package com.jifen.modules.admin;

import com.jifen.common.PageResult;
import com.jifen.modules.admin.dto.*;
import com.jifen.modules.order.dto.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
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
    "CREATE TABLE IF NOT EXISTS wj_admin (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  username VARCHAR(50) NOT NULL," +
    "  password VARCHAR(255) NOT NULL," +
    "  nickname VARCHAR(50)," +
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
    "CREATE TABLE IF NOT EXISTS wj_sys_config (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  config_key VARCHAR(50) UNIQUE," +
    "  config_value VARCHAR(255)," +
    "  description VARCHAR(255)," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")",
    "CREATE TABLE IF NOT EXISTS wj_sign_in (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  user_id BIGINT NOT NULL," +
    "  sign_date DATE NOT NULL," +
    "  points_awarded INT DEFAULT 0," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  UNIQUE (user_id, sign_date)" +
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
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        for (String tbl : List.of("wj_point_record", "wj_sign_in", "wj_order", "wj_sys_config", "wj_product", "wj_user", "wj_admin")) {
            jdbcTemplate.execute("DELETE FROM " + tbl);
            jdbcTemplate.execute("ALTER TABLE " + tbl + " ALTER COLUMN id RESTART WITH 1");
        }

        // Create an admin
        jdbcTemplate.update("INSERT INTO wj_admin (username, password, nickname, status) VALUES (?, ?, ?, ?)",
                "admin", passwordEncoder.encode("admin123"), "管理员", 1);

        // Create a test product
        jdbcTemplate.update("INSERT INTO wj_product (name, description, cover_image, points_required, stock, status) VALUES (?, ?, ?, ?, ?, ?)",
                "测试商品", "描述", "/img.jpg", 100, 10, 1);

        // Insert sys config
        jdbcTemplate.update("INSERT INTO wj_sys_config (config_key, config_value, description) VALUES (?, ?, ?)",
                "sign_in_points", "10", "签到奖励积分");
    }

    // ===== 商品管理 =====

    @Test
    void testListProducts() {
        ProductPageRequest req = new ProductPageRequest();
        PageResult<?> page = adminService.listProducts(req);
        assertEquals(1, page.getTotal());
    }

    @Test
    void testCreateProduct() {
        ProductFormRequest req = new ProductFormRequest();
        req.setName("新商品");
        req.setPointsRequired(200);
        req.setStock(5);
        Object id = adminService.createProduct(req);
        assertNotNull(id);

        ProductPageRequest listReq = new ProductPageRequest();
        assertEquals(2, adminService.listProducts(listReq).getTotal());
    }

    @Test
    void testToggleProductStatus() {
        adminService.toggleProductStatus(1L);
        // Should be off-shelf now
        ProductPageRequest req = new ProductPageRequest();
        req.setStatus(0);
        assertEquals(1, adminService.listProducts(req).getTotal());

        req.setStatus(1);
        assertEquals(0, adminService.listProducts(req).getTotal());
    }

    @Test
    void testDeleteProduct() {
        adminService.deleteProduct(1L);
        ProductPageRequest req = new ProductPageRequest();
        assertEquals(0, adminService.listProducts(req).getTotal());
    }

    // ===== 订单管理 =====

    @Test
    void testShipOrder() {
        // Create an order
        jdbcTemplate.update("INSERT INTO wj_order (order_no, user_id, product_id, product_name, points_spent, status) VALUES (?, ?, ?, ?, ?, ?)",
                "JF202605160001", 1L, 1L, "测试商品", 100, 0);

        ShipRequest shipReq = new ShipRequest();
        shipReq.setTrackingNo("SF123456789");
        adminService.shipOrder(1L, shipReq);

        OrderPageRequest listReq = new OrderPageRequest();
        listReq.setStatus(1);
        PageResult<OrderVO> page = adminService.listOrders(listReq);
        assertEquals(1, page.getTotal());
        assertEquals("SF123456789", page.getRecords().get(0).getTrackingNo());
    }

    // ===== 配置管理 =====

    @Test
    void testGetAndUpdateConfig() {
        Object config = adminService.getConfig();
        assertNotNull(config);
        assertTrue(config instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> configMap = (Map<String, String>) config;
        assertEquals("10", configMap.get("sign_in_points"));

        Map<String, String> update = new HashMap<>();
        update.put("sign_in_points", "20");
        adminService.updateConfig(update);

        Object updated = adminService.getConfig();
        @SuppressWarnings("unchecked")
        Map<String, String> updatedMap = (Map<String, String>) updated;
        assertEquals("20", updatedMap.get("sign_in_points"));
    }

    // ===== 统计看板 =====

    @Test
    void testDashboard() {
        DashboardVO vo = adminService.getDashboard();
        assertNotNull(vo);
        assertEquals(1, vo.getTotalProducts());
    }
}
