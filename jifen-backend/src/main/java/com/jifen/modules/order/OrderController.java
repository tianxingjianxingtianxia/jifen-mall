package com.jifen.modules.order;

import com.jifen.auth.UserContextUtil;
import com.jifen.common.PageResult;
import com.jifen.common.Result;
import com.jifen.modules.order.dto.CreateOrderRequest;
import com.jifen.modules.order.dto.OrderVO;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserContextUtil.getUserId();
        return Result.success(orderService.createOrder(userId, request));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContextUtil.getUserId();
        return Result.success(orderService.listOrders(userId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        Long userId = UserContextUtil.getUserId();
        return Result.success(orderService.getOrderDetail(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = UserContextUtil.getUserId();
        orderService.cancelOrder(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        Long userId = UserContextUtil.getUserId();
        orderService.confirmReceipt(id, userId);
        return Result.success();
    }
}
