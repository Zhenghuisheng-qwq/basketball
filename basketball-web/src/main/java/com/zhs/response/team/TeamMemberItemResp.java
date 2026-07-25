package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队成员项")
public class TeamMemberItemResp implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("场上位置中文名")
    private String positionName;

    @ApiModelProperty("球员认证状态")
    private Integer certificationStatus;

    @ApiModelProperty("头像 URL")
    private String avatarUrl;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("角色")
    private String roleType;

    @ApiModelProperty("阵容类型")
    private String rosterType;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("成员状态")
    private Integer membershipStatus;

    @ApiModelProperty("备注")
    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("加入时间")
    private Date joinTime;
}
