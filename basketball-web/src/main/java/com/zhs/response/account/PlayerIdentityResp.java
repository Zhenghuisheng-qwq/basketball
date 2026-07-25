package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球员身份信息")
public class PlayerIdentityResp implements Serializable {

    @ApiModelProperty("认证状态")
    private Integer certificationStatus;

    @ApiModelProperty("球员参赛资格状态：1 正常 / 2 受限")
    private Integer playerStatus;

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("游戏账号")
    private String gameAccount;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("认证通过时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date certifiedTime;

    @ApiModelProperty("最新申请编号")
    private String applicationNo;

    @ApiModelProperty("最新申请状态")
    private Integer applicationStatus;

    @ApiModelProperty("最新申请提交时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedTime;

    @ApiModelProperty("最新申请审核时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;

    @ApiModelProperty("最新申请审核说明")
    private String reviewRemark;
}
