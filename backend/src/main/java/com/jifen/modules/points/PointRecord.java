package com.jifen.modules.points;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jifen.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wj_point_record")
public class PointRecord extends BaseEntity {
    private Long userId;
    private Integer type;       // 1=获得, 2=消耗
    private String source;      // SIGN_IN / EXCHANGE / ORDER_CANCEL / EXPIRE / MANUAL_ADJUST / REFERRAL / FOLLOWUP
    private Integer points;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private Long relatedId;
    private String remark;
    private LocalDateTime expireTime; // 积分过期时间
}
