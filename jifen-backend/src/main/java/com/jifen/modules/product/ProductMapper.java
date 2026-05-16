package com.jifen.modules.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE wj_product SET stock = stock - 1, sale_count = sale_count + 1 WHERE id = #{productId} AND stock > 0")
    int updateStockDecrement(@Param("productId") Long productId);

    @Update("UPDATE wj_product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int updateStockIncrement(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE wj_product SET sale_count = sale_count + 1 WHERE id = #{productId}")
    int incrementSaleCount(@Param("productId") Long productId);

    @Update("UPDATE wj_product SET status = 0 WHERE id = #{productId} AND stock <= 0 AND status = 1")
    int autoOffShelfIfStockZero(@Param("productId") Long productId);
}
