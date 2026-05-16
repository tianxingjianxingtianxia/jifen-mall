package com.jifen.modules.points;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_sys_config")
public class SysConfig extends BaseEntity {
    private String configKey;
    private String configValue;
    private String description;
}
