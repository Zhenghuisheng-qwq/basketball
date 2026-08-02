package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionValidationItemVo {
    private String level;
    private String message;
    private Boolean passed;
}
