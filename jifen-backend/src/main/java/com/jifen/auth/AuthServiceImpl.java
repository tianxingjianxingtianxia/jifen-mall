package com.jifen.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jifen.auth.dto.AdminLoginRequest;
import com.jifen.auth.dto.AdminLoginResponse;
import com.jifen.auth.dto.LoginRequest;
import com.jifen.auth.dto.LoginResponse;
import com.jifen.auth.dto.RegisterRequest;
import com.jifen.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // Check if username already exists
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setPoints(0);
        user.setTotalEarned(0);
        user.setTotalSpent(0);
        user.setStatus(1);

        userMapper.insert(user);

        // Generate token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), false);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPoints(user.getPoints());
        return response;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user by username
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // Generate token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), false);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPoints(user.getPoints());
        return response;
    }

    @Override
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        // Find admin by username
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, request.getUsername());
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // Check status
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException("管理员已被禁用");
        }

        // Generate token
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), true);

        AdminLoginResponse response = new AdminLoginResponse();
        response.setToken(token);
        response.setUserId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setNickname(admin.getNickname());
        return response;
    }

    @Override
    public LoginResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPoints(user.getPoints());
        return response;
    }
}
