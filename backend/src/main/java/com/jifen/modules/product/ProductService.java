package com.jifen.modules.product;

import com.jifen.common.PageResult;
import com.jifen.modules.product.dto.ProductListVO;
import com.jifen.modules.product.dto.ProductVO;

public interface ProductService {

    /**
     * 商品列表（分页+搜索+排序）
     */
    PageResult<ProductListVO> listProducts(String keyword, String sortBy, int pageNum, int pageSize);

    /**
     * 商品详情
     */
    ProductVO getProductDetail(Long id);
}
