package com.zhs.basket.support.v4.template;

import lombok.Data;

@Data
public class StandardTemplateContext {
    private String seriesCode;
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
}
