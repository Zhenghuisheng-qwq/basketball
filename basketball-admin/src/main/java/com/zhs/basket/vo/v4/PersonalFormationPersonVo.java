package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class PersonalFormationPersonVo {
    private Long personalRegistrationId;
    private Long playerId;
    private Long userId;
    private String playerName;
    private String preferredPosition;
    private Date userCreateTime;
    private Long drawTeamId;
    private String assignmentRole;
}
