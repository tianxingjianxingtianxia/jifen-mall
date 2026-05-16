package com.jifen.modules.order;

import com.jifen.common.PageResult;
import com.jifen.modules.order.dto.CreateOrderRequest;
import com.jifen.modules.order.dto.OrderVO;

public interface OrderService {
    OrderVO createOrder(Long userId, CreateOrderRequest request);
    OrderVO getOrderDetail(Long id, Long userId);
    PageResult<OrderVO> listOrders(Long userId, Integer status, int pageNum, int pageSize);
    void cancelOrder(Long id, Long userId);
    void confirmReceipt(Long id, Long userId);
    int cancelExpiredOrders();
}
