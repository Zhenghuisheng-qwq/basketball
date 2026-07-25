package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CompetitionSeasonListVo {
    private Long id;
    private String seasonNo;
    private String seasonName;
    private Integer seasonYear;
    private Date startTime;
    private Date endTime;
    private Integer seasonStatus;
    private String seasonStatusName;
    private Integer publicStatus;
    private Integer tournamentCount;
    private Date createTime;
}
