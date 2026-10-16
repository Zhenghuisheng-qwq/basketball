package com.zhs.basket.vo.v4;

import com.zhs.basket.support.v4.version.RuleVersionView;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeamRosterWorkspaceVo {
    private Long divisionId;
    private String divisionName;
    private String registrationMode;
    private RuleVersionView ruleVersion;
    private String rosterStatus;
    private Long currentRosterVersionId;
    private Integer currentRosterVersionNo;
    private Integer formalCap;
    private Integer approvedCount;
    private Integer formalCount;
    private Integer candidateCount;
    private Integer excludedCount;
    private Integer unallocatedCount;
    private Integer shortTeamCount;
    private Integer lineupMinCount;
    private Boolean canEdit;
    private Boolean canLock;
    private List<TeamRosterItemVo> approvedList = new ArrayList<>();
    private List<TeamRosterItemVo> formalList = new ArrayList<>();
    private List<TeamRosterItemVo> candidateList = new ArrayList<>();
    private List<TeamRosterItemVo> excludedList = new ArrayList<>();
    private List<TeamRosterItemVo> unallocatedList = new ArrayList<>();
    private List<TeamRosterItemVo> rejectedList = new ArrayList<>();
}
