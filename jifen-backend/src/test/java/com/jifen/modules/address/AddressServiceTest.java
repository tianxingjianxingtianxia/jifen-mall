package com.jifen.modules.address;

import com.jifen.common.exception.BusinessException;
import com.jifen.modules.address.dto.AddressRequest;
import com.jifen.modules.address.dto.AddressVO;
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
    "CREATE TABLE IF NOT EXISTS wj_address (" +
    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
    "  user_id BIGINT NOT NULL," +
    "  receiver_name VARCHAR(50)," +
    "  receiver_phone VARCHAR(20)," +
    "  province VARCHAR(50)," +
    "  city VARCHAR(50)," +
    "  district VARCHAR(50)," +
    "  detail_address VARCHAR(500)," +
    "  is_default TINYINT DEFAULT 0," +
    "  is_deleted TINYINT DEFAULT 0," +
    "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
    "  update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
    ")"
})
public class AddressServiceTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Long userId = 1L;
    private final Long otherUserId = 2L;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM wj_address");
        jdbcTemplate.execute("ALTER TABLE wj_address ALTER COLUMN id RESTART WITH 1");
    }

    private AddressRequest createRequest(String name, String phone, int isDefault) {
        AddressRequest req = new AddressRequest();
        req.setReceiverName(name);
        req.setReceiverPhone(phone);
        req.setProvince("广东省");
        req.setCity("深圳市");
        req.setDistrict("南山区");
        req.setDetailAddress("科技园路1号");
        req.setIsDefault(isDefault);
        return req;
    }

    @Test
    void testAddAddress() {
        AddressVO vo = addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        assertNotNull(vo);
        assertNotNull(vo.getId());
        assertEquals("张三", vo.getReceiverName());
        assertEquals("13800138001", vo.getReceiverPhone());
        assertEquals("广东省", vo.getProvince());
        assertEquals("深圳市", vo.getCity());
        assertEquals("南山区", vo.getDistrict());
        assertEquals(0, vo.getIsDefault());
    }

    @Test
    void testAddDefaultAddress() {
        // Add a default address
        addressService.addAddress(userId, createRequest("张三", "13800138001", 1));
        // Add another default address — should clear the first one's default
        addressService.addAddress(userId, createRequest("李四", "13800138002", 1));

        List<AddressVO> list = addressService.listAddresses(userId);
        assertEquals(2, list.size());
        // Only the latest one should be default
        assertEquals(1, list.stream().filter(a -> a.getIsDefault() == 1).count());
        assertEquals("李四", list.get(0).getReceiverName()); // default first
    }

    @Test
    void testListAddresses() {
        addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        addressService.addAddress(userId, createRequest("李四", "13800138002", 1));

        List<AddressVO> list = addressService.listAddresses(userId);
        assertEquals(2, list.size());
        // Default should be first
        assertEquals("李四", list.get(0).getReceiverName());
        assertEquals(1, list.get(0).getIsDefault());
    }

    @Test
    void testListAddressesEmpty() {
        List<AddressVO> list = addressService.listAddresses(userId);
        assertTrue(list.isEmpty());
    }

    @Test
    void testListAddressesOnlyOwnUser() {
        addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        List<AddressVO> list = addressService.listAddresses(otherUserId);
        assertTrue(list.isEmpty());
    }

    @Test
    void testUpdateAddress() {
        AddressVO added = addressService.addAddress(userId, createRequest("张三", "13800138001", 0));

        AddressRequest updateReq = createRequest("张三改", "13800999999", 1);
        AddressVO updated = addressService.updateAddress(added.getId(), userId, updateReq);

        assertEquals("张三改", updated.getReceiverName());
        assertEquals("13800999999", updated.getReceiverPhone());
        assertEquals(1, updated.getIsDefault());
    }

    @Test
    void testUpdateAddressNotFound() {
        assertThrows(BusinessException.class, () -> {
            addressService.updateAddress(999L, userId, createRequest("测试", "13800138001", 0));
        });
    }

    @Test
    void testUpdateAddressNotOwn() {
        AddressVO added = addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        assertThrows(BusinessException.class, () -> {
            addressService.updateAddress(added.getId(), otherUserId, createRequest("测试", "13800138001", 0));
        });
    }

    @Test
    void testDeleteAddress() {
        AddressVO added = addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        addressService.deleteAddress(added.getId(), userId);

        List<AddressVO> list = addressService.listAddresses(userId);
        assertTrue(list.isEmpty());
    }

    @Test
    void testDeleteAddressNotOwn() {
        AddressVO added = addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        assertThrows(BusinessException.class, () -> {
            addressService.deleteAddress(added.getId(), otherUserId);
        });
    }

    @Test
    void testSetDefault() {
        AddressVO addr1 = addressService.addAddress(userId, createRequest("张三", "13800138001", 0));
        AddressVO addr2 = addressService.addAddress(userId, createRequest("李四", "13800138002", 1));

        // Set addr1 as default
        addressService.setDefault(addr1.getId(), userId);

        List<AddressVO> list = addressService.listAddresses(userId);
        assertEquals(2, list.size());
        assertEquals(1, list.stream().filter(a -> a.getIsDefault() == 1).count());
        assertEquals("张三", list.get(0).getReceiverName());
    }

    @Test
    void testSetDefaultNotFound() {
        assertThrows(BusinessException.class, () -> {
            addressService.setDefault(999L, userId);
        });
    }
}
