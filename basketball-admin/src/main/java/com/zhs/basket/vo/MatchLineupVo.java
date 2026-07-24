package com.zhs.basket.vo;

import lombok.Data;
import java.util.List;

@Data
public class MatchLineupVo {
    private Long matchId;
    private Long teamId;
    private String teamName;
    private List<MatchLineupPlayerVo> players;
}
