package com.zhs.basket.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamMembershipChangeDetailVo extends TeamMembershipChangeListVo {
    private Long subjectMemberId;
    private String gameId;
    private Integer certificationStatus;
    private String certificationStatusName;
    private Integer permanentMemberCount;
    private Integer eventRosterCount;
    private Integer pendingAddCount;
    private Integer pendingRemoveCount;
    private String rosterGapSummary;
}
