package com.zhs.basket.localdemo.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BasketLocalDemoAvatarSyncResp {
    private String marker;
    private boolean success;
    private String message;
    private int demoAccountCount;
    private int playerAvatarFileCount;
    private int teamLogoFileCount;
    private int teamLogoUpdatedCount;
    private int updatedCount;
    private int skippedCount;
    private int missingAvatarBefore;
    private int missingAvatarAfter;
    private long startedTime;
    private long finishedTime;
    private long durationMillis;
    private List<String> warnings = new ArrayList<>();
}
