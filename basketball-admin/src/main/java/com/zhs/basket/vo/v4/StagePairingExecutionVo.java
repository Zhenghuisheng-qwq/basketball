package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class StagePairingExecutionVo {
    private Long executionId;
    private Long stageId;
    private Integer executionNo;
    private String seedRule;
    private String drawRule;
    private String byeRule;
    private String placementRule;
    private String resolutionStatus;
    private Long sourceStageId;
    private Boolean hasUnresolvedPlaceholders;
    private Boolean participantResolved;
    private String status;
    private Long randomSeed;
    private Date generatedTime;
    private Date confirmedTime;
    private Integer participantCount;
    private Integer bracketCapacity;
    private Integer byeCount;
    private Integer firstRoundActual;
    private Integer totalActualSeries;
    private List<StagePairingEntryVo> entries = new ArrayList<>();
    private BracketPreviewVo bracketPreview;
}
