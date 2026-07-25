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
}
