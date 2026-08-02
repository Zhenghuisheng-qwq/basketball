package com.zhs.request.matchdata;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlayerMyScheduleQuery implements Serializable {

    /** 赛季/赛事 ID，空表示全部 */
    private Long seasonId;
    /** 比赛平台代码，空表示全部 */
    private String platformCode;
    /** PENDING / RUNNING / FINISHED / CANCELLED / ALL */
    private String matchStatusGroup;
    private String startDate;
    private String endDate;
    /** ASC / DESC */
    private String sortOrder;
    private Integer pageNum;
    private Integer pageSize;
}
