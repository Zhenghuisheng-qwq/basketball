package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("我的赛事页面身份上下文")
public class MyEventsContextResp implements Serializable {

    @ApiModelProperty("是否有球队")
    private Boolean hasTeam;

    @ApiModelProperty("当前球队主键")
    private String teamId;

    @ApiModelProperty("当前球队名称")
    private String teamName;

    @ApiModelProperty("球队角色：CAPTAIN / MEMBER / NONE")
    private String teamRole;

    @ApiModelProperty("是否当前球队队长")
    private Boolean isTeamCaptain;

    @ApiModelProperty("是否拥有有效队长授权")
    private Boolean hasActiveCaptainGrant;

    @ApiModelProperty("队长授权覆盖的赛季主键")
    private String captainGrantSeasonId;

    @ApiModelProperty("队长授权状态")
    private String captainGrantStatus;

    @ApiModelProperty("队长授权状态文案")
    private String captainGrantStatusText;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("队长授权过期时间")
    private Date captainGrantExpireTime;

    @ApiModelProperty("当前角色模式：NO_TEAM / TEAM_MEMBER / TEAM_CAPTAIN")
    private String currentRoleMode;

    @ApiModelProperty("同 currentRoleMode")
    private String mode;

    @ApiModelProperty("当前用户有效队长授权覆盖的赛季主键列表")
    private List<String> activeCaptainGrantSeasonIds;

    @ApiModelProperty("是否可代表球队管理报名")
    private Boolean canManageTeamRegistration;

    @ApiModelProperty("球队认证状态")
    private Integer teamCertificationStatus;

    @ApiModelProperty("球队比赛终端（历史字段，球队本体不绑定参赛端）")
    private String gameTerminal;
}
