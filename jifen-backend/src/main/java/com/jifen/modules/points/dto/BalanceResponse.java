package com.jifen.modules.points.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private Integer points;
    private Integer totalEarned;
    private Integer totalSpent;
}
