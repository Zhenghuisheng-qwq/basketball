package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开球队阵容项")
public class PublicTeamRosterItemVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    private String playerName;
    private String avatarUrl;
    private String positionCode;
    private String jerseyNo;
    private String roleType;
    private String rosterType;
    private Integer certificationStatus;
}
