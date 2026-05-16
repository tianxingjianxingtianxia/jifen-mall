package com.jifen.task;

import com.jifen.modules.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireTask {

    private final OrderService orderService;

    /**
     * 每分钟执行一次，检查并自动取消超时未完成的订单
     */
    @Scheduled(fixedRate = 60000)
    public void cancelExpiredOrders() {
        int count = orderService.cancelExpiredOrders();
        if (count > 0) {
            log.info("自动取消 {} 个超时订单并退回积分", count);
        }
    }
}
