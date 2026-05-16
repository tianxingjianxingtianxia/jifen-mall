package com.jifen.modules.product;

import com.jifen.common.PageResult;
import com.jifen.modules.product.dto.ProductListVO;
import com.jifen.modules.product.dto.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, statements = {
    "CREATE TABLE IF NOT EXISTS wj_product (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  name VARCHAR(100) NOT NULL," +
    "  description TEXT," +
    "  cover_image VARCHAR(500)," +
    "  points_required INT NOT NULL DEFAULT 0," +
    "  stock INT NOT NULL DEFAULT 0," +
    "  status TINYINT DEFAULT 1," +
    "  sort_order INT DEFAULT 0," +
    "  sale_count INT DEFAULT 0," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")",
    "CREATE TABLE IF NOT EXISTS wj_product_image (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  product_id BIGINT NOT NULL," +
    "  image_url VARCHAR(500) NOT NULL," +
    "  sort_order INT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")"
})
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean tables
        jdbcTemplate.execute("DELETE FROM wj_product_image");
        jdbcTemplate.execute("DELETE FROM wj_product");
        // Reset auto-increment for H2
        jdbcTemplate.execute("ALTER TABLE wj_product ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE wj_product_image ALTER COLUMN id RESTART WITH 1");

        // Insert test products
        // Product 1: low price, many stock
        jdbcTemplate.update(
            "INSERT INTO wj_product (name, description, cover_image, points_required, stock, status, sort_order, sale_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            "测试商品A", "商品A的描述", "/images/a.jpg", 100, 50, 1, 1, 10
        );

        // Product 2: high price, few stock
        jdbcTemplate.update(
            "INSERT INTO wj_product (name, description, cover_image, points_required, stock, status, sort_order, sale_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            "测试商品B", "商品B的描述", "/images/b.jpg", 500, 3, 1, 2, 5
        );

        // Product 3: medium price, zero stock
        jdbcTemplate.update(
            "INSERT INTO wj_product (name, description, cover_image, points_required, stock, status, sort_order, sale_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            "珍藏限量版", "限量版的描述", "/images/c.jpg", 300, 0, 1, 3, 20
        );

        // Product 4: off-shelf (should not appear in list)
        jdbcTemplate.update(
            "INSERT INTO wj_product (name, description, cover_image, points_required, stock, status, sort_order, sale_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            "已下架商品", "已下架", "/images/d.jpg", 200, 10, 0, 4, 0
        );

        // Insert product images for Product 1 (id=1)
        jdbcTemplate.update(
            "INSERT INTO wj_product_image (product_id, image_url, sort_order) VALUES (?, ?, ?)",
            1, "/images/a_1.jpg", 1
        );
        jdbcTemplate.update(
            "INSERT INTO wj_product_image (product_id, image_url, sort_order) VALUES (?, ?, ?)",
            1, "/images/a_2.jpg", 2
        );
    }

    // ===== 商品列表测试 =====

    @Test
    void testListAllProducts() {
        PageResult<ProductListVO> page = productService.listProducts(null, null, 1, 10);

        assertNotNull(page);
        assertEquals(3, page.getTotal()); // only 3 on-shelf products
        assertEquals(3, page.getRecords().size());
    }

    @Test
    void testListProductsSortByPointsAsc() {
        PageResult<ProductListVO> page = productService.listProducts(null, "points_asc", 1, 10);

        List<ProductListVO> records = page.getRecords();
        assertEquals(3, records.size());
        assertTrue(records.get(0).getPointsRequired() <= records.get(1).getPointsRequired());
        assertTrue(records.get(1).getPointsRequired() <= records.get(2).getPointsRequired());
        assertEquals("测试商品A", records.get(0).getName()); // 100 points first
    }

    @Test
    void testListProductsSortByPointsDesc() {
        PageResult<ProductListVO> page = productService.listProducts(null, "points_desc", 1, 10);

        List<ProductListVO> records = page.getRecords();
        assertEquals(3, records.size());
        assertTrue(records.get(0).getPointsRequired() >= records.get(1).getPointsRequired());
        assertTrue(records.get(1).getPointsRequired() >= records.get(2).getPointsRequired());
        assertEquals("测试商品B", records.get(0).getName()); // 500 points first
    }

    @Test
    void testListProductsSearchByKeyword() {
        PageResult<ProductListVO> page = productService.listProducts("珍藏", null, 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertEquals("珍藏限量版", page.getRecords().get(0).getName());
    }

    @Test
    void testListProductsSearchNoMatch() {
        PageResult<ProductListVO> page = productService.listProducts("不存在的商品", null, 1, 10);

        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void testListProductsPagination() {
        // Page with size 2 should return 2 products out of 3
        PageResult<ProductListVO> page1 = productService.listProducts(null, null, 1, 2);
        assertEquals(3, page1.getTotal()); // total is 3
        assertEquals(2, page1.getRecords().size());
        assertEquals(1, page1.getPageNum());
        assertEquals(2, page1.getPageSize());

        // Page 2 should have 1 product
        PageResult<ProductListVO> page2 = productService.listProducts(null, null, 2, 2);
        assertEquals(3, page2.getTotal());
        assertEquals(1, page2.getRecords().size());
        assertEquals(2, page2.getPageNum());
    }

    @Test
    void testListProductsDefaultSortWhenNull() {
        // When sortBy is null, should default to sort_order ASC
        PageResult<ProductListVO> page = productService.listProducts(null, null, 1, 10);

        List<ProductListVO> records = page.getRecords();
        assertEquals(3, records.size());
        // sort_order: 商品A=1, 商品B=2, 限量版=3 → A, B, 限量版
        // (default sort when sortBy is not specified)
    }

    @Test
    void testListProductsExcludesOffShelf() {
        // Off-shelf product should never appear
        PageResult<ProductListVO> page = productService.listProducts("已下架", null, 1, 10);
        assertEquals(0, page.getTotal());
    }

    // ===== 商品详情测试 =====

    @Test
    void testGetProductDetail() {
        ProductVO detail = productService.getProductDetail(1L);

        assertNotNull(detail);
        assertEquals(1L, detail.getId());
        assertEquals("测试商品A", detail.getName());
        assertEquals("商品A的描述", detail.getDescription());
        assertEquals("/images/a.jpg", detail.getCoverImage());
        assertEquals(100, detail.getPointsRequired());
        assertEquals(50, detail.getStock());
        assertEquals(10, detail.getSaleCount());
        assertEquals(1, detail.getStatus());
        assertNotNull(detail.getImages());
        assertEquals(2, detail.getImages().size());
        assertEquals("/images/a_1.jpg", detail.getImages().get(0));
        assertEquals("/images/a_2.jpg", detail.getImages().get(1));
        assertEquals("有货", detail.getStockStatus());
    }

    @Test
    void testGetProductDetailLowStock() {
        // Product 2 has stock=3 which should be "库存紧张"
        ProductVO detail = productService.getProductDetail(2L);

        assertNotNull(detail);
        assertEquals("库存紧张", detail.getStockStatus());
    }

    @Test
    void testGetProductDetailSoldOut() {
        // Product 3 has stock=0 which should be "已售罄"
        ProductVO detail = productService.getProductDetail(3L);

        assertNotNull(detail);
        assertEquals("已售罄", detail.getStockStatus());
    }

    @Test
    void testGetProductDetailNotFound() {
        ProductVO detail = productService.getProductDetail(999L);
        assertNull(detail);
    }

    @Test
    void testGetProductDetailOffShelfProduct() {
        // Off-shelf product should still be accessible by direct id lookup
        ProductVO detail = productService.getProductDetail(4L);
        assertNotNull(detail);
        assertEquals("已下架商品", detail.getName());
        assertEquals(0, detail.getStatus());
    }
}
