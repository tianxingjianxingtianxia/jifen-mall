package com.jifen.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer points = 0;
    private Integer totalEarned = 0;
    private Integer totalSpent = 0;
    private Integer status = 1;
}
