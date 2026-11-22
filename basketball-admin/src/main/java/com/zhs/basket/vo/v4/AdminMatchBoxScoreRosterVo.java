package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class AdminMatchBoxScoreRosterVo {
    private Long matchId;
    private String homeTeamName;
    private String awayTeamName;
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private String gameMode;
    private boolean periodRequired;
    private List<Player> homePlayers;
    private List<Player> awayPlayers;

    @Data
    public static class Player {
        private Long eventPlayerId;
        private String playerName;
        private String gameId;
        private String jerseyNo;
        private Integer isCaptain;
    }
}
