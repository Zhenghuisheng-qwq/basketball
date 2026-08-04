package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketDivisionGroupVo {
    private Long groupId;
    private String groupCode;
    private String groupName;
    private Integer groupOrder;
    private Integer qualifyCount;
    private List<BasketDivisionGroupTeamVo> teams;
}
