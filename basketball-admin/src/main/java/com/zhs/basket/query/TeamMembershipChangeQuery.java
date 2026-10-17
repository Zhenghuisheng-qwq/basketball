package com.zhs.basket.query;

import lombok.Data;

@Data
public class TeamMembershipChangeQuery {
    private String teamNo;
    private String teamName;
    private String changeStatus;
    private String changeType;
    private String playerKeyword;
}
