package com.jifen.modules.admin.dto;

import lombok.Data;

@Data
public class OrderPageRequest {
    private Integer status;
    private String orderNo;
    private int pageNum = 1;
    private int pageSize = 10;
}
