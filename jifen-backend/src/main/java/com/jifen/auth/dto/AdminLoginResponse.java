package com.jifen.auth.dto;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
}
