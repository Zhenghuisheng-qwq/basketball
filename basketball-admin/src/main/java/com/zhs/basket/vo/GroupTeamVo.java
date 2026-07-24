package com.zhs.basket.vo;

import lombok.Data;

@Data
public class GroupTeamVo {
    private Long id;
    private Long teamId;
    private String teamName;
    private Integer seedNo;
    private Integer drawOrder;
    private Integer groupTeamStatus;
}
