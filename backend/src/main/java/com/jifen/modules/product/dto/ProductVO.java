package com.jifen.modules.product.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private Integer pointsRequired;
    private Integer stock;
    private Integer status;
    private Integer sortOrder;
    private Integer saleCount;
    private List<String> images;
    private String stockStatus;
}
