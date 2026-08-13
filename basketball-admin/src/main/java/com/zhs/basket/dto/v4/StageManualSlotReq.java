package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StageManualSlotReq {
    private List<Slot> slots = new ArrayList<>();

    @Data
    public static class Slot {
        private Integer slotNo;
        private Long eventTeamId;
        private Boolean bye;
        private String sourceType;
        private Long sourceGroupId;
        private Integer sourceRank;
    }
}
