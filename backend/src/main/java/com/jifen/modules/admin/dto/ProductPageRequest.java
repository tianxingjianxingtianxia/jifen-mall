package com.jifen.modules.admin.dto;

import lombok.Data;

@Data
public class ProductPageRequest {
    private String keyword;
    private Integer status;
    private int pageNum = 1;
    private int pageSize = 10;
}
