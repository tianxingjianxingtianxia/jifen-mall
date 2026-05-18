package com.jifen.modules.admin;

import com.jifen.common.PageResult;
import com.jifen.modules.admin.dto.*;
import com.jifen.modules.order.dto.OrderVO;

import java.util.List;

public interface AdminService {

    // 商品管理
    PageResult<?> listProducts(ProductPageRequest request);
    Object createProduct(ProductFormRequest request);
    Object updateProduct(Long id, ProductFormRequest request);
    void toggleProductStatus(Long id);
    void deleteProduct(Long id);

    // 订单管理
    PageResult<OrderVO> listOrders(OrderPageRequest request);
    void shipOrder(Long id, ShipRequest request);

    // 配置管理
    Object getConfig();
    void updateConfig(java.util.Map<String, String> config);

    // 统计
    DashboardVO getDashboard();

    // === 导出 ===
    List<?> exportOrders(OrderPageRequest request);
    List<?> exportProducts(ProductPageRequest request);

    // === 客户积分管理 ===
    void adjustUserPoints(Long userId, int points, String source, String remark);
    PageResult<?> searchUsers(String keyword, int pageNum, int pageSize);
    void toggleUserStatus(Long userId);

    // === 积分有效期 ===
    List<?> getExpiredPoints();
    void cleanExpiredPoints();
}
