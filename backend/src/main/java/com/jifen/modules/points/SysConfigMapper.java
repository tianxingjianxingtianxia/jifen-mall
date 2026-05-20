package com.jifen.modules.points;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    @Select("SELECT config_value FROM wj_sys_config WHERE config_key = #{configKey} AND is_deleted = 0")
    String getValueByKey(@Param("configKey") String configKey);
}
