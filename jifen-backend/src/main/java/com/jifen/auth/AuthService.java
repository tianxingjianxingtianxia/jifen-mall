package com.jifen.auth;

import com.jifen.auth.dto.AdminLoginRequest;
import com.jifen.auth.dto.AdminLoginResponse;
import com.jifen.auth.dto.LoginRequest;
import com.jifen.auth.dto.LoginResponse;
import com.jifen.auth.dto.RegisterRequest;

public interface AuthService {
    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    AdminLoginResponse adminLogin(AdminLoginRequest request);

    LoginResponse getUserInfo(Long userId);
}
