package com.zhs.basket.localdemo.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BasketLocalDemoResetResp {
    private String marker;
    private boolean fullyCleared;
    private String message;
    private Long startedTime;
    private Long finishedTime;
    private Long durationMillis;
    private int deletedTotal;
    private List<String> deletedSummary = new ArrayList<>();
    private List<String> blockedRecords = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
