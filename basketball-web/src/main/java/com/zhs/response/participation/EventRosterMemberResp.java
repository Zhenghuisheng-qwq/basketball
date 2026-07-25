package com.zhs.response.participation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事阵容成员")
public class EventRosterMemberResp implements Serializable {

    private String rosterId;
    private String playerId;
    private String playerNo;
    private String nickname;
    private String platform;
    private String gameId;
    private String position;
    private String rosterRole;
    private String rosterRoleText;
    private Integer isCaptain;
    private Integer isStarting;
    private Integer rosterStatus;
    private String rosterStatusText;
    private String sourceType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effectiveTime;
}
