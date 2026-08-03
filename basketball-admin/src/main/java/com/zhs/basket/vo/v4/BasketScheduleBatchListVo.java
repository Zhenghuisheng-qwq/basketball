package com.zhs.basket.vo.v4;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class BasketScheduleBatchListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private String formatType;
    private String ruleCode;
    private Integer teamCount;
    private Integer matchCount;
    private String generateStatus;
    private String publishStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date generateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;
    private String remark;
}
