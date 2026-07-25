package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class CompetitionSeasonCreateReq {
    private String seasonName;
    private Integer seasonYear;
    private Date startTime;
    private Date endTime;
    private Integer seasonStatus;
    private Integer publicStatus;
    private String description;
}
