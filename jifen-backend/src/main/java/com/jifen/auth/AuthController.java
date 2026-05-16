package com.jifen.auth;

import com.jifen.auth.dto.AdminLoginRequest;
import com.jifen.auth.dto.AdminLoginResponse;
import com.jifen.auth.dto.LoginRequest;
import com.jifen.auth.dto.LoginResponse;
import com.jifen.auth.dto.RegisterRequest;
import com.jifen.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/admin/login")
    public Result<AdminLoginResponse> adminLogin(@RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = authService.adminLogin(request);
        return Result.success(response);
    }

    @GetMapping("/userinfo")
    public Result<LoginResponse> userInfo() {
        Long userId = UserContextUtil.getUserId();
        LoginResponse response = authService.getUserInfo(userId);
        return Result.success(response);
    }
}
