package com.zhs.request.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@ApiModel("我的赛事分页查询")
public class MyEventsSeasonPageQuery {

    @ApiModelProperty("赛事名称（模糊）")
    private String eventName;

    @ApiModelProperty("顶层赛季主键")
    private Long seasonId;

    @ApiModelProperty("游戏端/参赛端：PC/PS/XBOX/ALL，默认 ALL")
    private String platformCode;

    @ApiModelProperty("赛事状态：NOT_STARTED/OPEN/CLOSED/IN_PROGRESS/ENDED")
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("报名时间范围下限（筛选与报名窗口有交集的赛事）")
    private Date startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("报名时间范围上限（筛选与报名窗口有交集的赛事）")
    private Date endTime;

    @ApiModelProperty("页码，默认 1")
    private Integer pageNum;

    @ApiModelProperty("每页条数，默认 5")
    private Integer pageSize;

    @ApiModelProperty("页码（兼容旧参数 currentPage）")
    private Integer currentPage;
}
