package com.zhs.request.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("本队球员比赛数据提交请求")
public class PlayerMatchTeamPlayerStatsSubmitReq implements Serializable {

    private String submitterTeamId;
    private List<PlayerStatItem> playerStats;
    private String remark;

    @Data
    public static class PlayerStatItem implements Serializable {
        private String playerId;
        private String rosterId;
        private Integer points;
        private Integer rebounds;
        private Integer assists;
        private Integer steals;
        private Integer blocks;
        private Integer turnovers;
        private Integer fouls;
    }
}
