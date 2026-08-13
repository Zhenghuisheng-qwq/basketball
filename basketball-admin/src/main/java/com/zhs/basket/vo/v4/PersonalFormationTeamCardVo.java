package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalFormationTeamCardVo {
    private Long drawTeamId;
    private String temporaryTeamCode;
    private String temporaryTeamName;
    private Integer memberCount;
    private Long captainPersonalRegistrationId;
    private Long captainPlayerId;
    private Long captainUserId;
    private String captainPlayerName;
    private List<PersonalFormationPersonVo> members = new ArrayList<>();
}
