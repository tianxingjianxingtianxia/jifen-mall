package com.jifen.modules.product;

import com.jifen.common.PageResult;
import com.jifen.common.Result;
import com.jifen.modules.product.dto.ProductListVO;
import com.jifen.modules.product.dto.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 商品列表
     * GET /api/products?keyword=&sortBy=points_asc|points_desc&pageNum=1&pageSize=12
     */
    @GetMapping
    public Result<PageResult<ProductListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize) {
        PageResult<ProductListVO> page = productService.listProducts(keyword, sortBy, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 商品详情
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        ProductVO detail = productService.getProductDetail(id);
        if (detail == null) {
            return Result.error(404, "商品不存在");
        }
        return Result.success(detail);
    }
}
