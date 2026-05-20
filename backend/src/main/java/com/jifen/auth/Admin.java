package com.jifen.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_admin")
public class Admin extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private Integer status = 1;
}
