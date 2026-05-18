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
import com.jifen.modules.product.ProductImage;
import com.jifen.modules.product.ProductImageMapper;
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
    private final ProductImageMapper productImageMapper;

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
            // 查询商品图片列表
            List<ProductImage> imgs = productImageMapper.selectList(
                    Wrappers.lambdaQuery(ProductImage.class)
                            .eq(ProductImage::getProductId, p.getId())
                            .orderByAsc(ProductImage::getSortOrder));
            List<String> imgUrls = imgs.stream().map(ProductImage::getImageUrl).collect(Collectors.toList());
            m.put("images", imgUrls);
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

        // 保存多图片
        saveProductImages(product.getId(), request.getImages());

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

        // 先删除旧图片，再插入新图片
        productImageMapper.delete(Wrappers.lambdaQuery(ProductImage.class)
                .eq(ProductImage::getProductId, id));
        saveProductImages(id, request.getImages());

        return product.getId();
    }

    /**
     * 保存商品多图片
     */
    private void saveProductImages(Long productId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int i = 0; i < images.size(); i++) {
            ProductImage pi = new ProductImage();
            pi.setProductId(productId);
            pi.setImageUrl(images.get(i));
            pi.setSortOrder(i);
            productImageMapper.insert(pi);
        }
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

    // ===== 数据导出 =====

    @Override
    public List<?> exportOrders(OrderPageRequest request) {
        // 查询所有订单（不分页，按条件过滤）
        LambdaQueryWrapper<Order> wrapper = Wrappers.lambdaQuery(Order.class)
                .orderByDesc(Order::getCreateTime);

        if (request.getStatus() != null) {
            wrapper.eq(Order::getStatus, request.getStatus());
        }
        if (StrUtil.isNotBlank(request.getOrderNo())) {
            wrapper.like(Order::getOrderNo, request.getOrderNo());
        }

        List<Order> orders = orderMapper.selectList(wrapper);
        return orders.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderNo", o.getOrderNo());
            m.put("productName", o.getProductName());
            m.put("pointsSpent", o.getPointsSpent());
            m.put("receiverName", o.getReceiverName());
            m.put("receiverPhone", o.getReceiverPhone());
            m.put("receiverAddress", o.getReceiverAddress());
            m.put("status", o.getStatus());
            m.put("trackingNo", o.getTrackingNo() != null ? o.getTrackingNo() : "");
            m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : "");
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<?> exportProducts(ProductPageRequest request) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery(Product.class)
                .orderByDesc(Product::getCreateTime);

        if (StrUtil.isNotBlank(request.getKeyword())) {
            wrapper.like(Product::getName, request.getKeyword());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Product::getStatus, request.getStatus());
        }

        List<Product> products = productMapper.selectList(wrapper);
        return products.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("pointsRequired", p.getPointsRequired());
            m.put("stock", p.getStock());
            m.put("saleCount", p.getSaleCount());
            m.put("status", p.getStatus() == 1 ? "上架" : "下架");
            m.put("createTime", p.getCreateTime() != null ? p.getCreateTime().toString() : "");
            return m;
        }).collect(Collectors.toList());
    }

    // ===== 客户积分管理 =====

    @Override
    @Transactional
    public void adjustUserPoints(Long userId, int points, String source, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int beforeBalance = user.getPoints() != null ? user.getPoints() : 0;

        if (points > 0) {
            // 增加积分
            userMapper.addPoints(userId, points);
            int afterBalance = beforeBalance + points;

            PointRecord record = new PointRecord();
            record.setUserId(userId);
            record.setType(1); // 获得
            record.setSource(source != null ? source : "MANUAL_ADJUST");
            record.setPoints(points);
            record.setBalanceBefore(beforeBalance);
            record.setBalanceAfter(afterBalance);
            if (remark != null) {
                record.setRemark(remark);
            }
            pointRecordMapper.insert(record);

            log.info("[ADMIN] 用户{}增加{}积分，来源：{}，备注：{}", userId, points, source, remark);
        } else if (points < 0) {
            int deductPoints = -points;
            // 扣减积分
            int affected = userMapper.deductPoints(userId, deductPoints);
            if (affected == 0) {
                throw new BusinessException("积分不足，扣减失败");
            }
            int afterBalance = Math.max(beforeBalance - deductPoints, 0);

            PointRecord record = new PointRecord();
            record.setUserId(userId);
            record.setType(2); // 消耗
            record.setSource(source != null ? source : "MANUAL_ADJUST");
            record.setPoints(deductPoints);
            record.setBalanceBefore(beforeBalance);
            record.setBalanceAfter(afterBalance);
            if (remark != null) {
                record.setRemark(remark);
            }
            pointRecordMapper.insert(record);

            log.info("[ADMIN] 用户{}扣减{}积分，来源：{}，备注：{}", userId, deductPoints, source, remark);
        }
    }

    @Override
    public PageResult<?> searchUsers(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class)
                .orderByDesc(User::getCreateTime);

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getNickname, keyword));
        }

        IPage<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        IPage<Map<String, Object>> voPage = page.convert(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("phone", u.getPhone());
            m.put("points", u.getPoints() != null ? u.getPoints() : 0);
            m.put("totalEarned", u.getTotalEarned() != null ? u.getTotalEarned() : 0);
            m.put("totalSpent", u.getTotalSpent() != null ? u.getTotalSpent() : 0);
            m.put("status", u.getStatus() != null ? u.getStatus() : 1);
            m.put("createTime", u.getCreateTime() != null ? u.getCreateTime().toString() : "");
            return m;
        });
        return PageResult.of(voPage);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
        log.info("[ADMIN] 用户{}状态切换为{}", userId, user.getStatus());
    }

    // ===== 积分有效期 =====

    private int getPointsValidityDays() {
        String value = sysConfigMapper.getValueByKey("points_validity_days");
        if (value == null) {
            return 365;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("points_validity_days 配置值无效: {}", value);
            return 365;
        }
    }

    @Override
    public List<?> getExpiredPoints() {
        int validityDays = getPointsValidityDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(validityDays);

        LambdaQueryWrapper<PointRecord> wrapper = Wrappers.lambdaQuery(PointRecord.class)
                .eq(PointRecord::getType, 1) // 获得
                .lt(PointRecord::getCreateTime, cutoff)
                .orderByDesc(PointRecord::getCreateTime);

        List<PointRecord> records = pointRecordMapper.selectList(wrapper);
        return records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            User u = userMapper.selectById(r.getUserId());
            m.put("userId", r.getUserId());
            m.put("username", u != null ? u.getUsername() : "未知");
            m.put("points", r.getPoints());
            m.put("source", r.getSource());
            m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            m.put("expireTime", cutoff.toString());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cleanExpiredPoints() {
        int validityDays = getPointsValidityDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(validityDays);
        log.info("开始清理 {} 天之前的获得的积分，截止时间：{}", validityDays, cutoff);

        // 获取所有用户
        List<User> users = userMapper.selectList(
                Wrappers.lambdaQuery(User.class).eq(User::getStatus, 1));

        int expiredCount = 0;
        for (User user : users) {
            if (user.getPoints() == null || user.getPoints() <= 0) {
                continue;
            }

            // 查询该用户过期的积分记录（type=1 获得，创建时间早于截止日期）
            LambdaQueryWrapper<PointRecord> wrapper = Wrappers.lambdaQuery(PointRecord.class)
                    .eq(PointRecord::getUserId, user.getId())
                    .eq(PointRecord::getType, 1)
                    .lt(PointRecord::getCreateTime, cutoff);
            List<PointRecord> expiredRecords = pointRecordMapper.selectList(wrapper);
            int totalExpiredPoints = expiredRecords.stream()
                    .mapToInt(PointRecord::getPoints)
                    .sum();

            if (totalExpiredPoints <= 0) {
                continue;
            }

            // 最多扣除用户当前可用积分
            int expirePoints = Math.min(totalExpiredPoints, user.getPoints());
            userMapper.expirePoints(user.getId(), expirePoints);

            // 记录过期明细
            PointRecord record = new PointRecord();
            record.setUserId(user.getId());
            record.setType(2); // 消耗
            record.setSource("EXPIRE");
            record.setPoints(expirePoints);
            record.setBalanceBefore(user.getPoints());
            record.setBalanceAfter(Math.max(user.getPoints() - expirePoints, 0));
            record.setRemark("积分有效期过期清理（" + validityDays + "天）");
            pointRecordMapper.insert(record);

            expiredCount++;
            log.debug("用户 {} 过期积分 {}，剩余 {}", user.getUsername(), expirePoints,
                    Math.max(user.getPoints() - expirePoints, 0));
        }

        log.info("积分过期清理完成，处理 {} 个用户", expiredCount);
    }
}
