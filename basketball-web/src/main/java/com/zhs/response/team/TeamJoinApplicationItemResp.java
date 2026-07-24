package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("加入球队申请项")
public class TeamJoinApplicationItemResp implements Serializable {

    @ApiModelProperty("申请主键")
    private String applicationId;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("申请会员主键")
    private String applicantMemberId;

    @ApiModelProperty("申请人昵称")
    private String applicantNickname;

    @ApiModelProperty("申请原因")
    private String applyReason;

    @ApiModelProperty("申请状态")
    private Integer applicationStatus;

    @ApiModelProperty("申请状态描述")
    private String applicationStatusText;

    @ApiModelProperty("是否可取消")
    private Boolean canCancel;

    @ApiModelProperty("是否可处理")
    private Boolean canProcess;

    @ApiModelProperty("处理说明")
    private String processRemark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
