package com.jifen.modules.order;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jifen.auth.User;
import com.jifen.auth.UserMapper;
import com.jifen.common.PageResult;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.address.Address;
import com.jifen.modules.address.AddressMapper;
import com.jifen.modules.order.dto.CreateOrderRequest;
import com.jifen.modules.order.dto.OrderVO;
import com.jifen.modules.points.PointRecord;
import com.jifen.modules.points.PointRecordMapper;
import com.jifen.modules.product.Product;
import com.jifen.modules.product.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final PointRecordMapper pointRecordMapper;

    private static final int ORDER_EXPIRE_MINUTES = 15;
    private static final int REPEAT_EXCHANGE_DAYS = 30;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderRequest request) {
        // 1. 查询商品（含锁）
        Product product = productMapper.selectById(request.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStatus() != 1) {
            throw new BusinessException("商品已下架");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BusinessException("商品库存不足");
        }

        // 2. 30天内重复兑换限制
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(REPEAT_EXCHANGE_DAYS);
        LambdaQueryWrapper<Order> repeatWrapper = Wrappers.lambdaQuery(Order.class)
                .eq(Order::getUserId, userId)
                .eq(Order::getProductId, request.getProductId())
                .ne(Order::getStatus, 3) // 已取消的订单不算
                .ge(Order::getCreateTime, thirtyDaysAgo);
        Long repeatCount = orderMapper.selectCount(repeatWrapper);
        if (repeatCount > 0) {
            throw new BusinessException("同一商品30天内只能兑换1次");
        }

        // 3. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 4. 检查积分
        if (user.getPoints() < product.getPointsRequired()) {
            throw new BusinessException("积分不足");
        }

        // 5. 检查地址
        Address address = addressMapper.selectById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        // 6. 库存乐观锁扣减（防超卖）
        int updated = productMapper.updateStockDecrement(request.getProductId());
        if (updated == 0) {
            throw new BusinessException("商品库存不足");
        }

        // 7. 扣积分
        userMapper.deductPoints(userId, product.getPointsRequired());

        // 8. 生成订单号
        String orderNo = generateOrderNo();

        // 10. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(request.getProductId());
        order.setProductName(product.getName());
        order.setProductImage(product.getCoverImage());
        order.setPointsSpent(product.getPointsRequired());
        order.setAddressId(request.getAddressId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(
            StrUtil.join(" ", address.getProvince(), address.getCity(), address.getDistrict(), address.getDetailAddress())
        );
        order.setStatus(0); // 待发货
        order.setPaidAt(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES));
        orderMapper.insert(order);

        // 11. 写入积分变动记录
        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setType(2); // 消耗
        record.setSource("EXCHANGE");
        record.setPoints(product.getPointsRequired());
        record.setBalanceBefore(user.getPoints());
        record.setBalanceAfter(user.getPoints() - product.getPointsRequired());
        record.setRelatedId(order.getId());
        record.setRemark("兑换商品：" + product.getName());
        pointRecordMapper.insert(record);

        return toOrderVO(order);
    }

    @Override
    public OrderVO getOrderDetail(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }
        return toOrderVO(order);
    }

    @Override
    public PageResult<OrderVO> listOrders(Long userId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.lambdaQuery(Order.class)
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);

        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        IPage<Order> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        IPage<OrderVO> voPage = page.convert(this::toOrderVO);
        return PageResult.of(voPage);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("当前状态不允许取消");
        }
        if (order.getPaidAt() != null
                && LocalDateTime.now().isAfter(order.getPaidAt().plusMinutes(ORDER_EXPIRE_MINUTES))) {
            throw new BusinessException("已超过15分钟取消时限");
        }

        // 取消订单
        orderMapper.cancelOrder(id, "用户主动取消");

        // 退回积分
        userMapper.addPoints(userId, order.getPointsSpent());

        // 恢复库存
        productMapper.updateStockIncrement(order.getProductId(), 1);

        // 记录积分变动
        User user = userMapper.selectById(userId);
        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setType(1); // 获得
        record.setSource("ORDER_CANCEL");
        record.setPoints(order.getPointsSpent());
        record.setBalanceBefore(user.getPoints() - order.getPointsSpent());
        record.setBalanceAfter(user.getPoints());
        record.setRelatedId(order.getId());
        record.setRemark("取消订单退回积分：" + order.getProductName());
        pointRecordMapper.insert(record);
    }

    @Override
    @Transactional
    public void confirmReceipt(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("当前状态不允许确认收货");
        }

        order.setStatus(2);
        order.setConfirmedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public int cancelExpiredOrders() {
        List<Order> expired = orderMapper.selectExpiredOrders(LocalDateTime.now());
        if (expired.isEmpty()) {
            return 0;
        }

        for (Order order : expired) {
            try {
                orderMapper.cancelOrder(order.getId(), "超时未完成，自动取消");
                userMapper.addPoints(order.getUserId(), order.getPointsSpent());
                productMapper.updateStockIncrement(order.getProductId(), 1);

                User user = userMapper.selectById(order.getUserId());
                PointRecord record = new PointRecord();
                record.setUserId(order.getUserId());
                record.setType(1);
                record.setSource("ORDER_CANCEL");
                record.setPoints(order.getPointsSpent());
                record.setBalanceBefore(user != null ? user.getPoints() - order.getPointsSpent() : 0);
                record.setBalanceAfter(user != null ? user.getPoints() : 0);
                record.setRelatedId(order.getId());
                record.setRemark("超时自动取消退回积分：" + order.getProductName());
                pointRecordMapper.insert(record);
            } catch (Exception e) {
                log.error("取消过期订单失败, orderId={}", order.getId(), e);
            }
        }
        return expired.size();
    }

    // ===== 辅助方法 =====

    private String generateOrderNo() {
        return "JF" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss")
                + String.format("%04d", (int)(Math.random() * 10000));
    }

    private OrderVO toOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setProductId(order.getProductId());
        vo.setProductName(order.getProductName());
        vo.setProductImage(order.getProductImage());
        vo.setPointsSpent(order.getPointsSpent());
        vo.setAddressId(order.getAddressId());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setTrackingNo(order.getTrackingNo());
        vo.setCancelReason(order.getCancelReason());
        vo.setCancelTime(order.getCancelTime());
        vo.setPaidAt(order.getPaidAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setConfirmedAt(order.getConfirmedAt());
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待发货";
            case 1 -> "已发货";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }
}
