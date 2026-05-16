package com.jifen.modules.product.dto;

import lombok.Data;

@Data
public class ProductListVO {
    private Long id;
    private String name;
    private String coverImage;
    private Integer pointsRequired;
    private Integer stock;
    private Integer saleCount;
    private String stockStatus;
}
