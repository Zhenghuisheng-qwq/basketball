package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketCompetitionSeason {
    private Long id;
    private String seasonNo;
    private String seasonName;
    private Integer seasonYear;
    private Date startTime;
    private Date endTime;
    private Integer seasonStatus;
    private Integer publicStatus;
    private String description;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
