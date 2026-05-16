package com.jifen.modules.product;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wj_product_image")
public class ProductImage {
    private Long id;
    private Long productId;
    private String imageUrl;
    private Integer sortOrder;
    private java.time.LocalDateTime createTime;
}
