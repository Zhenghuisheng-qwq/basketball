package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RosterSyncResultVo {
    private Long registrationId;
    private Long teamId;
    private String teamName;
    private boolean skipped;
    private String skipReason;
    private int kept;
    private int added;
    private int removed;
    private int eligibleCount;
    private List<RegistrationGapMemberVo> blocked = new ArrayList<>();
}
