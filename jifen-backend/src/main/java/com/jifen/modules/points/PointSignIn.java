package com.jifen.modules.points;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_sign_in")
public class PointSignIn extends BaseEntity {
    private Long userId;
    private LocalDate signDate;
    private Integer pointsAwarded = 0;
}
