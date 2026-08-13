package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StageExternalRankingReq {
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long eventTeamId;
        private Integer rankingNo;
    }
}
