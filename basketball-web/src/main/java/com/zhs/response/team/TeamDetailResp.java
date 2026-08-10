package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队详情")
public class TeamDetailResp implements Serializable {

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("所属赛事 Division ID")
    private String divisionId;

    @ApiModelProperty("Division 名称")
    private String divisionName;

    @ApiModelProperty("Division 状态")
    private String divisionStatus;

    @ApiModelProperty("平台代码")
    private String divisionPlatformCode;

    @ApiModelProperty("比赛模式")
    private String gameMode;

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("系列主键")
    private String seriesId;

    @ApiModelProperty("系列名称")
    private String seriesName;

    @ApiModelProperty("成员关系状态")
    private Integer membershipStatus;

    @ApiModelProperty("离队原因")
    private String leaveReason;

    @ApiModelProperty("是否可管理（写权限）")
    private Boolean canManage;

    @ApiModelProperty("是否历史球队（对当前用户）")
    private Boolean isHistorical;

    @ApiModelProperty("展示用业务状态文案")
    private String displayStatusText;

    @ApiModelProperty("报名状态（如有）")
    private String registrationStatus;

    @ApiModelProperty("球队编号")
    private String teamNo;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("Logo 文件主键")
    private String logoFileId;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("地区编码")
    private String regionCode;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("球队介绍")
    private String description;

    @ApiModelProperty("当前成员数")
    private Integer currentMemberCount;

    @ApiModelProperty("最大成员数")
    private Integer maxMemberCount;

    @ApiModelProperty("球队状态")
    private Integer teamStatus;

    @ApiModelProperty("审核状态")
    private Integer auditStatus;

    @ApiModelProperty("公开状态")
    private Integer publicStatus;

    @ApiModelProperty("是否队长")
    private Boolean isCaptain;

    @ApiModelProperty("我的角色")
    private String myRole;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("成立日期")
    private Date establishedDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
