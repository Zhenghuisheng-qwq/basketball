package com.zhs.basket.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeamCaptainDeskVo {
    private Long teamId;
    private String teamNo;
    private String teamName;
    private Long divisionId;
    private Integer teamStatus;
    private List<Member> members = new ArrayList<>();

    @Data
    public static class Member {
        private Long teamMemberId;
        private Long memberId;
        private Long playerId;
        private String roleType;
        private String name;
        private String playerNo;
        private boolean seatCaptain;
    }
}
