package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class GroupListVo {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String groupCode;
    private String groupName;
    private Integer qualifyCount;
    private Integer sequenceNo;
    private Integer groupStatus;
    private String groupStatusName;
    private Integer teamCount;
    private Date createTime;
}
