package com.zhs.basket.query;

import lombok.Data;

@Data
public class TeamProfileChangeQuery {
    private String changeNo;
    private String teamName;
    private String submitUser;
    private Integer auditStatus;
    private String auditStage;
    private String beginTime;
    private String endTime;
}
