package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketStageTeamSyncResultVo {

    private int totalEventTeam;

    private int existingStageTeam;

    private int insertedStageTeam;

    private int skippedStageTeam;
}
