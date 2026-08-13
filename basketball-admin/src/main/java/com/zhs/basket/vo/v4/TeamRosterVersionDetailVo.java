package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TeamRosterVersionDetailVo {
    private Long rosterVersionId;
    private Long divisionId;
    private Integer versionNo;
    private String status;
    private Integer isCurrent;
    private Integer formalCount;
    private Integer candidateCount;
    private Integer excludedCount;
    private Date lockedTime;
    private List<TeamRosterItemVo> formalList = new ArrayList<>();
    private List<TeamRosterItemVo> candidateList = new ArrayList<>();
    private List<TeamRosterItemVo> excludedList = new ArrayList<>();
}
