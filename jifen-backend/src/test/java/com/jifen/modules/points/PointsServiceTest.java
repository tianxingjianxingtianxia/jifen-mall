package com.jifen.modules.points;

import com.jifen.auth.User;
import com.jifen.auth.UserMapper;
import com.jifen.common.PageResult;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.points.dto.BalanceResponse;
import com.jifen.modules.points.dto.PointRecordVO;
import com.jifen.modules.points.dto.SignInResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

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
    ")",
    "CREATE TABLE IF NOT EXISTS wj_sys_config (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  config_key VARCHAR(50) UNIQUE," +
    "  config_value VARCHAR(255)," +
    "  description VARCHAR(255)," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")"
})
public class PointsServiceTest {

    @Autowired
    private PointsService pointsService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Clean tables
        jdbcTemplate.execute("DELETE FROM wj_sign_in");
        jdbcTemplate.execute("DELETE FROM wj_point_record");
        jdbcTemplate.execute("DELETE FROM wj_sys_config");
        jdbcTemplate.execute("DELETE FROM wj_user");
        // Reset auto-increment
        jdbcTemplate.execute("ALTER TABLE wj_sign_in ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE wj_point_record ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE wj_sys_config ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE wj_user ALTER COLUMN id RESTART WITH 1");

        // Create a test user
        String encodedPwd = passwordEncoder.encode("password123");
        jdbcTemplate.update(
            "INSERT INTO wj_user (username, password, nickname, points, total_earned, total_spent) VALUES (?, ?, ?, ?, ?, ?)",
            "testuser", encodedPwd, "Test User", 100, 100, 0
        );
        testUserId = jdbcTemplate.queryForObject(
            "SELECT id FROM wj_user WHERE username = ?", Long.class, "testuser"
        );

        // Insert default sys_config
        jdbcTemplate.update(
            "INSERT INTO wj_sys_config (config_key, config_value, description) VALUES (?, ?, ?)",
            "sign_in_points", "10", "签到奖励积分"
        );
    }

    // ===== 签到测试 =====

    @Test
    void testSignInSuccess() {
        SignInResponse response = pointsService.signIn(testUserId);

        assertNotNull(response);
        assertTrue(response.getTodaySigned());
        assertEquals(10, response.getPoints());
        assertEquals(110, response.getTotalPoints());

        // Verify user points updated
        User user = userMapper.selectById(testUserId);
        assertEquals(110, user.getPoints());
        assertEquals(110, user.getTotalEarned());
        assertEquals(0, user.getTotalSpent());

        // Verify sign_in record
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wj_sign_in WHERE user_id = ? AND sign_date = CURRENT_DATE",
            Integer.class, testUserId
        );
        assertEquals(1, count);

        // Verify point_record
        Integer recordCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wj_point_record WHERE user_id = ? AND type = 1 AND source = 'SIGN_IN'",
            Integer.class, testUserId
        );
        assertEquals(1, recordCount);
    }

    @Test
    void testSignInDuplicate() {
        // First sign-in
        pointsService.signIn(testUserId);

        // Second sign-in should throw
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            pointsService.signIn(testUserId);
        });
        assertTrue(exception.getMessage().contains("今日已签到") || exception.getMessage().contains("已签到"));
    }

    // ===== 今日签到状态查询 =====

    @Test
    void testTodaySignStatusNotSigned() {
        boolean signed = pointsService.isTodaySigned(testUserId);
        assertFalse(signed);
    }

    @Test
    void testTodaySignStatusSigned() {
        pointsService.signIn(testUserId);
        boolean signed = pointsService.isTodaySigned(testUserId);
        assertTrue(signed);
    }

    // ===== 积分余额查询 =====

    @Test
    void testBalanceQuery() {
        BalanceResponse balance = pointsService.getBalance(testUserId);

        assertNotNull(balance);
        assertEquals(100, balance.getPoints());
        assertEquals(100, balance.getTotalEarned());
        assertEquals(0, balance.getTotalSpent());
    }

    @Test
    void testBalanceAfterSignIn() {
        pointsService.signIn(testUserId);

        BalanceResponse balance = pointsService.getBalance(testUserId);
        assertEquals(110, balance.getPoints());
        assertEquals(110, balance.getTotalEarned());
        assertEquals(0, balance.getTotalSpent());
    }

    // ===== 积分明细分页 =====

    @Test
    void testRecordsEmpty() {
        PageResult<PointRecordVO> page = pointsService.getRecords(testUserId, 1, 10);

        assertNotNull(page);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void testRecordsPagination() {
        // Sign in to create records
        pointsService.signIn(testUserId);

        PageResult<PointRecordVO> page = pointsService.getRecords(testUserId, 1, 10);

        assertNotNull(page);
        assertEquals(1, page.getTotal());
        assertEquals(1, page.getRecords().size());

        PointRecordVO record = page.getRecords().get(0);
        assertEquals(10, record.getPoints());
        assertEquals(1, record.getType()); // 1=获得
        assertEquals("SIGN_IN", record.getSource());
        assertNotNull(record.getCreateTime());
    }

    @Test
    void testRecordsOrderByCreateTimeDesc() {
        // Create two records
        pointsService.signIn(testUserId);

        // Simulate another record by inserting directly
        jdbcTemplate.update(
            "INSERT INTO wj_point_record (user_id, type, source, points, balance_before, balance_after) VALUES (?, ?, ?, ?, ?, ?)",
            testUserId, 2, "EXCHANGE", 50, 110, 60
        );

        PageResult<PointRecordVO> page = pointsService.getRecords(testUserId, 1, 10);

        assertEquals(2, page.getTotal());
        // First record should be the most recent (EXCHANGE, since we inserted after sign-in)
        assertEquals("EXCHANGE", page.getRecords().get(0).getSource());
        assertEquals("SIGN_IN", page.getRecords().get(1).getSource());
    }
}
