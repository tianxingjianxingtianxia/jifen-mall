package com.jifen.modules.order.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;
}
