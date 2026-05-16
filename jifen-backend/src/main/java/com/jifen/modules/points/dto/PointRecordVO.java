package com.jifen.modules.points.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PointRecordVO {
    private Long id;
    private Integer points;
    private Integer type;
    private String source;
    private String remark;
    private LocalDateTime createTime;
}
