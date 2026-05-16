package com.jifen.modules.admin;

import com.jifen.auth.UserContextUtil;
import com.jifen.common.PageResult;
import com.jifen.common.Result;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.admin.dto.*;
import com.jifen.modules.order.dto.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ===== 商品管理 =====

    @GetMapping("/products")
    public Result<PageResult<?>> listProducts(ProductPageRequest request) {
        checkAdmin();
        return Result.success(adminService.listProducts(request));
    }

    @PostMapping("/products")
    public Result<Object> createProduct(@RequestBody ProductFormRequest request) {
        checkAdmin();
        return Result.success(adminService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public Result<Object> updateProduct(@PathVariable Long id, @RequestBody ProductFormRequest request) {
        checkAdmin();
        return Result.success(adminService.updateProduct(id, request));
    }

    @PutMapping("/products/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        checkAdmin();
        adminService.toggleProductStatus(id);
        return Result.success();
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        checkAdmin();
        adminService.deleteProduct(id);
        return Result.success();
    }

    // ===== 订单管理 =====

    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> listOrders(OrderPageRequest request) {
        checkAdmin();
        return Result.success(adminService.listOrders(request));
    }

    @PutMapping("/orders/{id}/ship")
    public Result<Void> shipOrder(@PathVariable Long id, @RequestBody ShipRequest request) {
        checkAdmin();
        adminService.shipOrder(id, request);
        return Result.success();
    }

    // ===== 配置管理 =====

    @GetMapping("/config")
    public Result<Object> getConfig() {
        checkAdmin();
        return Result.success(adminService.getConfig());
    }

    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody Map<String, String> config) {
        checkAdmin();
        adminService.updateConfig(config);
        return Result.success();
    }

    // ===== 统计看板 =====

    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        checkAdmin();
        return Result.success(adminService.getDashboard());
    }

    private void checkAdmin() {
        if (!UserContextUtil.getIsAdmin()) {
            throw new BusinessException(403, "无管理员权限");
        }
    }
}
