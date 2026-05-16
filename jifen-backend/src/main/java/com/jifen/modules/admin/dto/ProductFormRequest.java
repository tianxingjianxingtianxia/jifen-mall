package com.jifen.modules.admin.dto;

import lombok.Data;

@Data
public class ProductFormRequest {
    private String name;
    private String description;
    private String coverImage;
    private Integer pointsRequired;
    private Integer stock;
    private Integer sortOrder;
}
