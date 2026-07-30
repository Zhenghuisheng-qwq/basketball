package com.zhs.basket.vo;

import lombok.Data;

@Data
public class ScheduleGenerateResultVo {
    private Long batchId;
    private String batchNo;
    private Integer matchCount;
    private Integer eventTeamCount;
}
