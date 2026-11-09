package com.zhs.basket.resp;

import lombok.Data;

@Data
public class SiteThemeBindingResp {
    private Long id;
    private String scopeType;
    private Long scopeId;
    private Long themeId;
    private String themeName;
    private String seasonName;
    private Integer themeVersion;
    private String scopeLabel;
}
