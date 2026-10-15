package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BasketRosterChangeListVo {
    private Long id;
    private Long registrationId;
    private Long divisionId;
    private Long teamId;
    private String teamName;
    private String changeStatus;
    private Integer baseLineupVersion;
    private Integer currentLineupVersion;
    private Date createTime;
    private String auditRemark;
    private List<Long> currentPlayerIds;
    private List<Long> targetPlayerIds;
    private List<String> addedNames;
    private List<String> removedNames;
}
