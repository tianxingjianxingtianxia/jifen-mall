package com.jifen.modules.order;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_order")
public class Order extends BaseEntity {
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
    private Integer status;       // 0-待发货 1-已发货 2-已完成 3-已取消
    private String trackingNo;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime expireTime;
}
