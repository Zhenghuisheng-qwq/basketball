package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("可继承历史球队摘要")
public class InheritableTeamSummaryResp implements Serializable {

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队编号")
    private String teamNo;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("简称")
    private String shortName;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("审核状态")
    private Integer auditStatus;

    @ApiModelProperty("审核状态描述")
    private String auditStatusText;

    @ApiModelProperty("当前成员数")
    private Integer currentMemberCount;

    @ApiModelProperty("上一届赛事名称")
    private String previousEventName;

    @ApiModelProperty("挂在这支球队上的荣誉条数")
    private Integer honorCount;

    @ApiModelProperty("是否可继承")
    private Boolean canInherit;

    @ApiModelProperty("不可继承原因")
    private String cannotInheritReason;

    @ApiModelProperty("是否为报名入口的默认推荐。只取最近一次报名审核通过、且当前仍有管理权的球队")
    private Boolean recommended;

    @ApiModelProperty("最近一次报名审核通过时间。草稿、驳回、取消不参与推荐排序")
    private Date lastApprovedTime;
}
