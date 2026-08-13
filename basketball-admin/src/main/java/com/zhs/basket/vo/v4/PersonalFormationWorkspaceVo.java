package com.zhs.basket.vo.v4;

import com.zhs.basket.support.v4.version.RuleVersionView;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalFormationWorkspaceVo {
    private Long divisionId;
    private String divisionName;
    private Long seasonId;
    private String seasonTitle;
    private String seriesCode;
    private String seriesName;
    private String gameMode;
    private String registrationMode;
    private String ruleStatus;
    private String rosterStatus;
    private String formationStatus;
    private Long currentBatchId;
    private Long currentRosterVersionId;
    private Integer currentRosterVersionNo;
    private Integer minTeamSize;
    private Integer maxTeamSize;
    private Integer minimumValidPersonCount;
    private Integer validPersonCount;
    private Integer temporaryTeamCount;
    private Integer unassignedCount;
    private Boolean canGenerate;
    private Boolean canAdjust;
    private Boolean canLock;
    private RuleVersionView ruleVersion;
    private List<PersonalFormationPersonVo> unassignedList = new ArrayList<>();
    private List<PersonalFormationTeamCardVo> teams = new ArrayList<>();
}
