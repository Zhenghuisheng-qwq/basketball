package com.zhs.basket.vo;

import lombok.Data;

@Data
public class SeasonRosterChangeRecordVo {
    private Long id; private Long seasonId; private Long changeRequestId; private String changeType; private String changeTypeName; private Long playerId; private Long sourceEventTeamId; private Long targetEventTeamId; private String beforeSnapshot; private String afterSnapshot; private String operatorName; private String operateReason; private java.util.Date effectiveTime; private java.util.Date createTime;
}
