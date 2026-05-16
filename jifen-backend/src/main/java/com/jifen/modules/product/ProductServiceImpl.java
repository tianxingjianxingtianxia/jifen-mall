package com.jifen.modules.product;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jifen.common.PageResult;
import com.jifen.modules.product.dto.ProductListVO;
import com.jifen.modules.product.dto.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Override
    public PageResult<ProductListVO> listProducts(String keyword, String sortBy, int pageNum, int pageSize) {
        // Build query condition: status=1 (on-shelf) AND is_deleted=0 (MyBatis-Plus logic delete handles is_deleted)
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery(Product.class)
                .eq(Product::getStatus, 1);

        // Keyword search on name (LIKE)
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Product::getName, keyword);
        }

        // Sort
        if ("points_asc".equals(sortBy)) {
            wrapper.orderByAsc(Product::getPointsRequired);
        } else if ("points_desc".equals(sortBy)) {
            wrapper.orderByDesc(Product::getPointsRequired);
        } else {
            // Default sort by sort_order ASC
            wrapper.orderByAsc(Product::getSortOrder);
        }

        // Paginate
        IPage<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // Convert to VO
        List<ProductListVO> records = page.getRecords().stream()
                .map(this::toProductListVO)
                .collect(Collectors.toList());

        // Build PageResult manually (type mismatch with generic IPage)
        PageResult<ProductListVO> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }

        // Query images
        LambdaQueryWrapper<ProductImage> imageWrapper = Wrappers.lambdaQuery(ProductImage.class)
                .eq(ProductImage::getProductId, id)
                .orderByAsc(ProductImage::getSortOrder);
        List<ProductImage> images = productImageMapper.selectList(imageWrapper);

        List<String> imageUrls = images.stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        // Build VO
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setCoverImage(product.getCoverImage());
        vo.setPointsRequired(product.getPointsRequired());
        vo.setStock(product.getStock());
        vo.setStatus(product.getStatus());
        vo.setSortOrder(product.getSortOrder());
        vo.setSaleCount(product.getSaleCount());
        vo.setImages(imageUrls);
        vo.setStockStatus(resolveStockStatus(product.getStock()));

        return vo;
    }

    /**
     * Convert Product entity to ProductListVO
     */
    private ProductListVO toProductListVO(Product product) {
        ProductListVO vo = new ProductListVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setCoverImage(product.getCoverImage());
        vo.setPointsRequired(product.getPointsRequired());
        vo.setStock(product.getStock());
        vo.setSaleCount(product.getSaleCount());
        vo.setStockStatus(resolveStockStatus(product.getStock()));
        return vo;
    }

    /**
     * Resolve stock status text
     */
    private String resolveStockStatus(Integer stock) {
        if (stock == null || stock <= 0) {
            return "已售罄";
        } else if (stock <= 5) {
            return "库存紧张";
        } else {
            return "有货";
        }
    }
}
