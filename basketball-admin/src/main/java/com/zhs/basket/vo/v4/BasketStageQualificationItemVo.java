package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketStageQualificationItemVo {
    private Long qualificationId;
    private Long eventTeamId;
    private String teamName;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private String qualificationType;
    private String qualificationTypeName;
    private Integer sourceRank;
    private String qualificationStatus;
}
