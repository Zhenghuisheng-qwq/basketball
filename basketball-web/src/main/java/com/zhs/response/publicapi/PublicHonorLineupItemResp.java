package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开冠军荣誉阵容项（不含 memberId/accountId）")
public class PublicHonorLineupItemResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("阵容主键")
    private Long lineupId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("球队主键")
    private Long teamId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("球员主键")
    private Long playerId;

    @ApiModelProperty("展示名（game_nickname）")
    private String playerName;

    @ApiModelProperty("头像 URL")
    private String avatarUrl;

    @ApiModelProperty("阵容角色")
    private String roleType;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
