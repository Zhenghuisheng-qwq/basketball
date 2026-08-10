package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队邀请项")
public class TeamInvitationItemResp implements Serializable {

    @ApiModelProperty("邀请主键")
    private String invitationId;

    @ApiModelProperty("邀请编号")
    private String invitationNo;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("所属赛事 Division ID")
    private String divisionId;

    @ApiModelProperty("Division 名称")
    private String divisionName;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("系列名称")
    private String seriesName;

    @ApiModelProperty("平台代码")
    private String platformCode;

    @ApiModelProperty("比赛模式")
    private String gameMode;

    @ApiModelProperty("邀请人用户主键")
    private String inviterMemberId;

    @ApiModelProperty("被邀请用户主键")
    private String inviteeMemberId;

    @ApiModelProperty("邀请人用户ID")
    private String inviterUserId;

    @ApiModelProperty("被邀请用户ID")
    private String inviteeUserId;

    @ApiModelProperty("被邀请人昵称")
    private String inviteeNickname;

    @ApiModelProperty("被邀请人头像 URL")
    private String inviteeAvatarUrl;

    @ApiModelProperty("邀请说明")
    private String invitationMessage;

    @ApiModelProperty("邀请状态")
    private Integer invitationStatus;

    @ApiModelProperty("邀请状态描述")
    private String invitationStatusText;

    @ApiModelProperty("是否可响应")
    private Boolean canRespond;

    @ApiModelProperty("是否可接受邀请")
    private Boolean canAccept;

    @ApiModelProperty("不可接受原因")
    private String cannotAcceptReason;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("过期时间")
    private Date expireTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
