package com.jifen.modules.address;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    @Update("UPDATE wj_address SET is_default = 0 WHERE user_id = #{userId} AND is_default = 1 AND is_deleted = 0")
    int clearDefaultByUserId(@Param("userId") Long userId);
}
