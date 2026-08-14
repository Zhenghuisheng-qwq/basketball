package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RosterSummaryVo {
    private String rosterStatus;
    private String rosterStatusName;
    private Long currentRosterVersionId;
    private Integer currentRosterVersionNo;
    private Integer formalCount;
    private Integer candidateCount;
    private Integer excludedCount;
    private boolean locked;
    private List<RosterVersionHistoryItemVo> versions = new ArrayList<>();
}
