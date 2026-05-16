package com.jifen.auth;

import com.jifen.auth.dto.AdminLoginRequest;
import com.jifen.auth.dto.LoginRequest;
import com.jifen.auth.dto.LoginResponse;
import com.jifen.auth.dto.RegisterRequest;
import com.jifen.common.exception.BusinessException;
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
    "CREATE TABLE IF NOT EXISTS wj_admin (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  username VARCHAR(50) NOT NULL," +
    "  password VARCHAR(255) NOT NULL," +
    "  nickname VARCHAR(50)," +
    "  status TINYINT DEFAULT 1," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")"
})
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        // Clean tables before each test
        jdbcTemplate.execute("DELETE FROM wj_user");
        jdbcTemplate.execute("DELETE FROM wj_admin");
        // Reset auto-increment
        jdbcTemplate.execute("ALTER TABLE wj_user ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE wj_admin ALTER COLUMN id RESTART WITH 1");

        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setNickname("Test User");
    }

    @Test
    void testRegisterSuccess() {
        LoginResponse response = authService.register(validRegisterRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getNickname());
        assertEquals(0, response.getPoints());
    }

    @Test
    void testRegisterDuplicateUsername() {
        // First registration should succeed
        authService.register(validRegisterRequest);

        // Second registration with same username should throw BusinessException
        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("testuser");
        duplicate.setPassword("anotherpass");
        duplicate.setNickname("Another");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.register(duplicate);
        });
        assertTrue(exception.getMessage().contains("已存在"));
    }

    @Test
    void testLoginSuccess() {
        // Register first
        authService.register(validRegisterRequest);

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("Test User", response.getNickname());
    }

    @Test
    void testLoginWrongPassword() {
        // Register first
        authService.register(validRegisterRequest);

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });
        assertTrue(exception.getMessage().contains("密码错误") || exception.getMessage().contains("错误"));
    }

    @Test
    void testAdminLoginSuccess() {
        // Insert an admin directly for testing
        String encodedPassword = passwordEncoder.encode("admin123");
        jdbcTemplate.update(
            "INSERT INTO wj_admin (username, password, nickname, status) VALUES (?, ?, ?, ?)",
            "admin", encodedPassword, "Admin", 1
        );

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        var response = authService.adminLogin(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("Admin", response.getNickname());
    }

    @Test
    void testLoginUserNotFound() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("password123");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.login(loginRequest);
        });
        assertTrue(exception.getMessage().contains("不存在") || exception.getMessage().contains("未找到"));
    }

    @Test
    void testGetUserInfo() {
        // Register first
        LoginResponse registerResponse = authService.register(validRegisterRequest);

        // Get user info
        LoginResponse userInfo = authService.getUserInfo(registerResponse.getUserId());

        assertNotNull(userInfo);
        assertEquals("testuser", userInfo.getUsername());
        assertEquals("Test User", userInfo.getNickname());
        assertEquals(0, userInfo.getPoints());
    }
}
