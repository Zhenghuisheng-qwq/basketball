package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队摘要")
public class TeamSummaryResp implements Serializable {

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队编号")
    private String teamNo;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

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

    @ApiModelProperty("招募状态（同 publicStatus）")
    private Integer recruitStatus;

    @ApiModelProperty("成员数量（同 currentMemberCount）")
    private Integer memberCount;

    @ApiModelProperty("是否可申请加入")
    private Boolean canApply;

    @ApiModelProperty("不可申请原因")
    private String cannotApplyReason;

    @ApiModelProperty("我的角色")
    private String myRole;

    @ApiModelProperty("是否队长")
    private Boolean isCaptain;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
