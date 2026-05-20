package com.jifen.modules.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_product")
public class Product extends BaseEntity {
    private String name;
    private String description;
    private String coverImage;
    private Integer pointsRequired;
    private Integer stock;
    private Integer status;
    private Integer sortOrder;
    private Integer saleCount;
}
