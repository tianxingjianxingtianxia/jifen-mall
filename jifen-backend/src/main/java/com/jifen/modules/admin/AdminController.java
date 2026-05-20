package com.jifen.modules.admin;

import com.jifen.auth.UserContextUtil;
import com.jifen.common.PageResult;
import com.jifen.common.Result;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.admin.dto.*;
import com.jifen.modules.order.dto.OrderVO;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    // ===== 数据导出 =====

    @GetMapping("/orders/export")
    public void exportOrders(OrderPageRequest request, HttpServletResponse response) {
        checkAdmin();
        List<?> list = adminService.exportOrders(request);
        writeCsvResponse(response, list,
                "订单号,用户,商品,消耗积分,收货人,手机号,收货地址,状态,物流单号,创建时间",
                new String[]{"orderNo", "productName", "pointsSpent", "receiverName",
                        "receiverPhone", "receiverAddress", "status", "trackingNo", "createTime"},
                "订单导出.csv");
    }

    @GetMapping("/products/export")
    public void exportProducts(ProductPageRequest request, HttpServletResponse response) {
        checkAdmin();
        List<?> list = adminService.exportProducts(request);
        writeCsvResponse(response, list,
                "名称,所需积分,库存,销量,状态,创建时间",
                new String[]{"name", "pointsRequired", "stock", "saleCount", "status", "createTime"},
                "商品导出.csv");
    }

    // ===== 客户积分管理 =====

    @GetMapping("/users")
    public Result<PageResult<?>> searchUsers(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        checkAdmin();
        return Result.success(adminService.searchUsers(keyword, pageNum, pageSize));
    }

    @PutMapping("/users/{userId}/points")
    public Result<Void> adjustUserPoints(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        checkAdmin();
        Object pointsObj = body.get("points");
        if (pointsObj == null) {
            throw new BusinessException("points 字段不能为空");
        }
        int points;
        try {
            points = Integer.parseInt(pointsObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("points 必须为整数");
        }
        String source = body.get("source") != null ? body.get("source").toString() : "MANUAL_ADJUST";
        String remark = body.get("remark") != null ? body.get("remark").toString() : "管理员手动调整";
        adminService.adjustUserPoints(userId, points, source, remark);
        return Result.success();
    }

    @PutMapping("/users/{userId}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long userId) {
        checkAdmin();
        adminService.toggleUserStatus(userId);
        return Result.success();
    }

    @GetMapping("/users/export")
    public void exportUsers(HttpServletResponse response) {
        checkAdmin();
        List<?> list = adminService.exportUsers();
        writeCsvResponse(response, list,
                "ID,用户名,昵称,手机号,积分,注册时间",
                new String[]{"id", "username", "nickname", "phone", "points", "createTime"},
                "用户导出.csv");
    }

    // ===== 积分有效期 =====

    @GetMapping("/expired-points")
    public Result<List<?>> getExpiredPoints() {
        checkAdmin();
        return Result.success(adminService.getExpiredPoints());
    }

    @PostMapping("/points/clean-expired")
    public Result<Void> cleanExpiredPoints() {
        checkAdmin();
        adminService.cleanExpiredPoints();
        return Result.success();
    }

    // ===== 私有方法 =====

    private void checkAdmin() {
        if (!UserContextUtil.getIsAdmin()) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    /**
     * 写 CSV 到 HttpServletResponse（UTF-8 BOM 编码，兼容 Excel 中文）
     */
    @SuppressWarnings("unchecked")
    private void writeCsvResponse(HttpServletResponse response, List<?> dataList,
                                   String headerLine, String[] fields, String fileName) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try {
            // 写入 UTF-8 BOM
            OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writer.write(headerLine);
            writer.write("\r\n");

            for (Object item : dataList) {
                Map<String, Object> map = (Map<String, Object>) item;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    Object val = map.get(fields[i]);
                    if (val != null) {
                        String str = val.toString();
                        // 如果包含逗号、引号或换行符，用双引号包裹
                        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
                            str = "\"" + str.replace("\"", "\"\"") + "\"";
                        }
                        sb.append(str);
                    }
                    if (i < fields.length - 1) {
                        sb.append(",");
                    }
                }
                sb.append("\r\n");
                writer.write(sb.toString());
            }

            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("导出 CSV 失败", e);
        }
    }
}
