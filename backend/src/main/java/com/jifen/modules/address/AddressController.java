package com.jifen.modules.address;

import com.jifen.auth.UserContextUtil;
import com.jifen.common.Result;
import com.jifen.modules.address.dto.AddressRequest;
import com.jifen.modules.address.dto.AddressVO;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public Result<List<AddressVO>> list() {
        Long userId = UserContextUtil.getUserId();
        return Result.success(addressService.listAddresses(userId));
    }

    @PostMapping
    public Result<AddressVO> add(@Valid @RequestBody AddressRequest request) {
        Long userId = UserContextUtil.getUserId();
        return Result.success(addressService.addAddress(userId, request));
    }

    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        Long userId = UserContextUtil.getUserId();
        return Result.success(addressService.updateAddress(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContextUtil.getUserId();
        addressService.deleteAddress(id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = UserContextUtil.getUserId();
        addressService.setDefault(id, userId);
        return Result.success();
    }
}
