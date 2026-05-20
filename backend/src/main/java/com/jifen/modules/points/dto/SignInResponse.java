package com.jifen.modules.points.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignInResponse {
    private Boolean todaySigned;
    private Integer points;
    private Integer totalPoints;
}
