package com.jifen.modules.order.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer pointsSpent;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Integer status;
    private String statusText;
    private String trackingNo;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
