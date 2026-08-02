package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
@ApiModel("我的赛程分页")
public class PlayerMySchedulePageResp implements Serializable {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<PlayerMatchTaskResp> records = Collections.emptyList();
    private PlayerMatchTaskResp nextMatch;
    /** 未完成球员认证时为 true */
    private Boolean certificationRequired;
    /** 已认证但无赛事快照时为 true */
    private Boolean noParticipation;
}
