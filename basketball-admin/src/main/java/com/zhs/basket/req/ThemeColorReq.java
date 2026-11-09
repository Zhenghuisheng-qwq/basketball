package com.zhs.basket.req;

import lombok.Data;

@Data
public class ThemeColorReq {
    private String primary;
    private String secondary;
    private String accent;
    private String content;
    private String card;
    private String tableHead;
    private String border;
    private String text;
    private String textMuted;
    private String onAccent;
}
