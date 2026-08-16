package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("basket_discipline_record")
public class BasketPublicDisciplineRow {

    @TableId
    private Long id;
    private String targetType;
    private Long seasonId;
    private Long teamId;
    private Long eventTeamId;
    private Long playerId;
    private Long memberId;
    private String penaltyCode;
    private String recordStatus;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Integer isDelete;
}
