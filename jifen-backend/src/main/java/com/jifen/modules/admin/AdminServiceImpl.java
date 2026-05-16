package com.jifen.modules.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jifen.auth.User;
import com.jifen.auth.UserMapper;
import com.jifen.common.PageResult;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.admin.dto.*;
import com.jifen.modules.order.Order;
import com.jifen.modules.order.OrderMapper;
import com.jifen.modules.order.OrderServiceImpl;
import com.jifen.modules.order.dto.OrderVO;
import com.jifen.modules.points.*;
import com.jifen.modules.product.Product;
import com.jifen.modules.product.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;
    private final PointSignInMapper pointSignInMapper;
    private final PointRecordMapper pointRecordMapper;
    private final OrderServiceImpl orderService; // 用于订单VO转换

    // ===== 商品管理 =====

    @Override
    public PageResult<?> listProducts(ProductPageRequest request) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery(Product.class)
                .orderByDesc(Product::getCreateTime);

        if (StrUtil.isNotBlank(request.getKeyword())) {
            wrapper.like(Product::getName, request.getKeyword());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Product::getStatus, request.getStatus());
        }

        IPage<Product> page = productMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);

        IPage<Map<String, Object>> voPage = page.convert(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("description", p.getDescription());
            m.put("coverImage", p.getCoverImage());
            m.put("pointsRequired", p.getPointsRequired());
            m.put("stock", p.getStock());
            m.put("status", p.getStatus());
            m.put("sortOrder", p.getSortOrder());
            m.put("saleCount", p.getSaleCount());
            m.put("createTime", p.getCreateTime());
            m.put("updateTime", p.getUpdateTime());
            return m;
        });
        return PageResult.of(voPage);
    }

    @Override
    @Transactional
    public Object createProduct(ProductFormRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCoverImage(request.getCoverImage());
        product.setPointsRequired(request.getPointsRequired());
        product.setStock(request.getStock());
        product.setStatus(1); // 默认上架
        product.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        product.setSaleCount(0);
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    @Transactional
    public Object updateProduct(Long id, ProductFormRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCoverImage(request.getCoverImage());
        product.setPointsRequired(request.getPointsRequired());
        product.setStock(request.getStock());
        product.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : product.getSortOrder());
        productMapper.updateById(product);
        return product.getId();
    }

    @Override
    @Transactional
    public void toggleProductStatus(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(product.getStatus() == 1 ? 0 : 1);
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        productMapper.deleteById(id); // 逻辑删除
    }

    // ===== 订单管理 =====

    @Override
    public PageResult<OrderVO> listOrders(OrderPageRequest request) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.lambdaQuery(Order.class)
                .orderByDesc(Order::getCreateTime);

        if (request.getStatus() != null) {
            wrapper.eq(Order::getStatus, request.getStatus());
        }
        if (StrUtil.isNotBlank(request.getOrderNo())) {
            wrapper.like(Order::getOrderNo, request.getOrderNo());
        }

        IPage<Order> page = orderMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        IPage<OrderVO> voPage = page.convert(orderService::toOrderVO);

        // 填充用户名称
        if (voPage.getRecords() != null && !voPage.getRecords().isEmpty()) {
            List<Long> userIds = voPage.getRecords().stream()
                    .map(OrderVO::getUserId)
                    .collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectBatchIds(userIds);
                Map<Long, String> userMap = users.stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));
                for (OrderVO vo : voPage.getRecords()) {
                    vo.setUserName(userMap.getOrDefault(vo.getUserId(), ""));
                }
            }
        }

        return PageResult.of(voPage);
    }

    @Override
    @Transactional
    public void shipOrder(Long id, ShipRequest request) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("当前状态不允许发货");
        }
        order.setStatus(1);
        order.setTrackingNo(request.getTrackingNo());
        order.setShippedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    // ===== 配置管理 =====

    @Override
    public Object getConfig() {
        List<SysConfig> list = sysConfigMapper.selectList(null);
        Map<String, String> configMap = new LinkedHashMap<>();
        for (SysConfig c : list) {
            configMap.put(c.getConfigKey(), c.getConfigValue());
        }
        return configMap;
    }

    @Override
    @Transactional
    public void updateConfig(Map<String, String> config) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            LambdaQueryWrapper<SysConfig> wrapper = Wrappers.lambdaQuery(SysConfig.class)
                    .eq(SysConfig::getConfigKey, entry.getKey());
            SysConfig existing = sysConfigMapper.selectOne(wrapper);
            if (existing != null) {
                existing.setConfigValue(entry.getValue());
                sysConfigMapper.updateById(existing);
            }
        }
    }

    // ===== 统计看板 =====

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();

        // 用户总数
        vo.setTotalUsers(userMapper.selectCount(Wrappers.lambdaQuery(User.class).eq(User::getStatus, 1)));
        // 商品总数（上架）
        vo.setTotalProducts(productMapper.selectCount(
                Wrappers.lambdaQuery(Product.class).eq(Product::getStatus, 1)));
        // 订单总数
        vo.setTotalOrders(orderMapper.selectCount(null));
        // 今日签到
        vo.setTodaySignIns(pointSignInMapper.countByDate(LocalDate.now()));
        // 待处理订单
        vo.setPendingOrders(orderMapper.selectCount(
                Wrappers.lambdaQuery(Order.class).eq(Order::getStatus, 0)));
        // 积分统计
        List<PointRecord> allRecords = pointRecordMapper.selectList(null);
        vo.setTotalPointsEarned(allRecords.stream()
                .filter(r -> r.getType() == 1).mapToInt(PointRecord::getPoints).sum());
        vo.setTotalPointsSpent(allRecords.stream()
                .filter(r -> r.getType() == 2).mapToInt(PointRecord::getPoints).sum());

        return vo;
    }
}
