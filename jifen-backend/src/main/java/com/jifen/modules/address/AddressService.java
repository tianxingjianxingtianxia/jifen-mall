package com.jifen.modules.address;

import com.jifen.modules.address.dto.AddressRequest;
import com.jifen.modules.address.dto.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> listAddresses(Long userId);

    AddressVO addAddress(Long userId, AddressRequest request);

    AddressVO updateAddress(Long id, Long userId, AddressRequest request);

    void deleteAddress(Long id, Long userId);

    void setDefault(Long id, Long userId);
}
