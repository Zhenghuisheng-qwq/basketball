package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionStageQualification {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Long groupId;
    private Long eventTeamId;
    private String qualificationType;
    private Integer sourceRank;
    private Long targetStageId;
    private String qualificationStatus;
    private Long confirmedBy;
    private Date confirmedTime;
    private String activeUniqueKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
