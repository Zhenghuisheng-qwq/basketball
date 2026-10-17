package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class StandardTemplateApplyReq {
    private String templateCode;
    private String registrationMode;
    private String gameMode;
    private Integer roundRobinCycles;
    private Integer playoffSlots;
    private Integer groupCount;
    private Integer groupCycles;
    private Integer advancePerGroup;
    private String groupingMode;
    private String eliminationPlacement;
    private String seedRule;
    private String drawRule;
    private String byeRule;
    private Integer regularBestOf;
    private Integer knockoutBestOf;
    private Integer semifinalBestOf;
    private Integer finalBestOf;
    private Integer winPoints;
    private Integer lossPoints;
    /** 重新发布时，确认未开赛赛程失效。草稿应用忽略该字段。 */
    private Boolean confirmScheduleImpact;
}
