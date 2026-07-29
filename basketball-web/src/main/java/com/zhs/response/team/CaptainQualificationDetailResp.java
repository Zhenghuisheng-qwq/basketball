package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("队长资格详情")
public class CaptainQualificationDetailResp implements Serializable {

    @ApiModelProperty("是否拥有有效资格")
    private Boolean hasQualification;

    @ApiModelProperty("资格编号")
    private String qualificationNo;

    @ApiModelProperty("资格状态")
    private Integer status;

    @ApiModelProperty("资格状态描述")
    private String statusText;

    @ApiModelProperty("缴费确认状态")
    private Integer paymentConfirmed;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("生效时间")
    private Date validFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("失效时间")
    private Date validTo;

    @ApiModelProperty("当前有效标记")
    private Integer isCurrent;

    @ApiModelProperty("是否可创建球队")
    private Boolean canCreateTeam;

    @ApiModelProperty("运行时是否有效（与 canCreateTeam 一致）")
    private Boolean runtimeValid;

    @ApiModelProperty("无效原因")
    private String invalidReason;

    @ApiModelProperty("授权赛事赛季主键")
    private String seasonId;

    @ApiModelProperty("授权赛事赛季名称")
    private String seasonName;

    @ApiModelProperty("绑定球队主键")
    private String teamId;

    @ApiModelProperty("绑定球队名称")
    private String teamName;

    @ApiModelProperty("是否可管理已绑定球队")
    private Boolean canManageTeam;

    @ApiModelProperty("是否可管理指定球队（传入 teamId 时计算）")
    private Boolean canManageCurrentTeam;

    @ApiModelProperty("无法管理指定球队的原因")
    private String cannotManageReason;

    @ApiModelProperty("是否可继承历史球队")
    private Boolean canInheritTeam;

    @ApiModelProperty("授权是否已收回（赛事周期结束等）")
    private Boolean grantRevoked;
}
