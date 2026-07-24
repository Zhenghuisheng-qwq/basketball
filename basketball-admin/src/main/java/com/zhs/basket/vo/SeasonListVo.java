package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class SeasonListVo {
    private Long id;
    private String seasonNo;
    private String seasonName;
    private Integer seasonYear;
    private Integer seasonStatus;
    private String seasonStatusName;
    private Date registrationStartTime;
    private Date registrationEndTime;
}
