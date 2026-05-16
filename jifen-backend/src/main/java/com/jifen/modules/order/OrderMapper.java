package com.jifen.modules.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT order_no FROM wj_order WHERE order_no = #{orderNo} AND is_deleted = 0")
    String existsByOrderNo(@Param("orderNo") String orderNo);

    @Update("UPDATE wj_order SET status = 3, cancel_reason = #{reason}, cancel_time = NOW() WHERE id = #{id} AND status = 0 AND is_deleted = 0")
    int cancelOrder(@Param("id") Long id, @Param("reason") String reason);

    @Select("SELECT * FROM wj_order WHERE expire_time IS NOT NULL AND expire_time <= #{now} AND status = 0 AND is_deleted = 0 LIMIT 100")
    List<Order> selectExpiredOrders(@Param("now") LocalDateTime now);
}
