package com.jifen.modules.admin.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DashboardVO {
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long todaySignIns;
    private long pendingOrders;
    private long totalPointsEarned;
    private long totalPointsSpent;
    private Map<String, Object> exchangeTrend;
}
