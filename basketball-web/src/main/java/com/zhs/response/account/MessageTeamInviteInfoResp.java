package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("消息关联的球队邀请信息")
public class MessageTeamInviteInfoResp implements Serializable {

    @ApiModelProperty("邀请主键")
    private String inviteId;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队 Logo URL")
    private String teamLogo;

    @ApiModelProperty("邀请状态")
    private Integer inviteStatus;

    @ApiModelProperty("邀请状态描述")
    private String inviteStatusText;

    @ApiModelProperty("是否可响应")
    private Boolean canRespond;

    @ApiModelProperty("是否可接受邀请")
    private Boolean canAccept;

    @ApiModelProperty("不可接受原因")
    private String cannotAcceptReason;

    @ApiModelProperty("邀请说明")
    private String invitationMessage;

    @ApiModelProperty("邀请人昵称")
    private String inviterName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("邀请时间")
    private Date inviteTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("过期时间")
    private Date expireTime;
}
