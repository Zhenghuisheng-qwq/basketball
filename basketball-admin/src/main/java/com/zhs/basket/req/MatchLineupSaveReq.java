package com.zhs.basket.req;

import lombok.Data;
import java.util.List;

@Data
public class MatchLineupSaveReq {
    private List<LineupPlayerItem> players;

    @Data
    public static class LineupPlayerItem {
        private Long registrationPlayerId;
        private String jerseyNo;
        private Integer isStarting;
        private Integer captainFlag;
        private String rosterRole;
        private String positionCode;
        private Integer sortNo;
    }
}
