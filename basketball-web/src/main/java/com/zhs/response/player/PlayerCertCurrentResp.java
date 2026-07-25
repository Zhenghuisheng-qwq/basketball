package com.zhs.response.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("球员认证当前状态响应")
public class PlayerCertCurrentResp implements Serializable {

    @ApiModelProperty("是否已有球员档案")
    private Boolean hasPlayerProfile;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("球员档案认证状态")
    private Integer certificationStatus;

    @ApiModelProperty("当前申请主键")
    private String currentApplicationId;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("申请状态")
    private Integer applicationStatus;

    @ApiModelProperty("申请状态文案")
    private String applicationStatusText;

    @ApiModelProperty("提交时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedTime;

    @ApiModelProperty("审核完成时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;

    @ApiModelProperty("驳回原因")
    private String rejectReason;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("游戏账号")
    private String gameAccount;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("认证材料列表")
    private List<PlayerCertMaterialResp> materials;

    @ApiModelProperty("是否可编辑")
    private Boolean canEdit;

    @ApiModelProperty("是否可提交")
    private Boolean canSubmit;
}
