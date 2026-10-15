package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class BasketCaptainGrantSyncPreviewVo {
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long id;
        private Long divisionId;
        private String playerName;
        private String grantStatus;
        private String validToMode;
        private Date currentValidTo;
        private Date targetValidTo;
        private Date divisionMatchEndTime;
        private Date seasonEndTime;
        private String eventEndSource;
        private boolean applicable;
        private String message;
    }
}
