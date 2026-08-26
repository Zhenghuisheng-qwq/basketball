package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@Data
public class PublicHomeDataFocusVo implements Serializable {

    private String seriesCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private PublicHomePlayerLeaderVo pointsLeader;

    private PublicHomePlayerLeaderVo assistsLeader;

    private PublicHomePlayerLeaderVo reboundsLeader;

    private PublicHomeFocusTeamVo focusTeam;
}
