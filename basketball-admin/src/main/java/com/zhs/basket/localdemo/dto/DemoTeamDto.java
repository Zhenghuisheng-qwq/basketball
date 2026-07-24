package com.zhs.basket.localdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemoTeamDto {
    private String teamNo;
    private String teamName;
    private String englishName;
    private String shortName;
    private String description;
    /** 小组标识：A / B / C / D */
    private String groupKey;
}
