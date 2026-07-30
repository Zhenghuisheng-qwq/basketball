package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketScheduleGenerateBatch {
    private Long id;
    private String batchNo;
    private Long eventId;
    private Long ruleId;
    private String ruleCode;
    private String ruleVersion;
    private Integer eventTeamCount;
    private Integer matchCount;
    private Integer generateStatus;
    private Integer publishStatus;
    private Long generatedBy;
    private Date generatedTime;
    private Date publishTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
