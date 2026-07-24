package com.zhs.basket.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SeasonEndCheckResp {
    private Long seasonId;
    private boolean canConfirm;
    private Integer confirmedMatchCount;
    private Integer unfinishedMatchCount;
    private Integer reopenedMatchCount;
    private Integer finishedUnconfirmedCount;
    private Integer teamSeasonStatCount;
    private boolean requireConfirmRemark;
    private List<String> blockers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
