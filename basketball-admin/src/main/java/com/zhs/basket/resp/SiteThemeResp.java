package com.zhs.basket.resp;

import com.zhs.basket.req.ThemeColorReq;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SiteThemeResp {
    private Long id;
    private String themeName;
    private String description;
    private String bgColor;
    private Long bgImageFileId;
    private String bgImageUrl;
    private Long mobileBgImageFileId;
    private String mobileBgImageUrl;
    private String bgDisplay;
    private ThemeColorReq colors;
    private Integer themeStatus;
    private String themeStatusName;
    private Integer hasDraft;
    private Integer versionNo;
    private String pendingChangeLabel;
    private List<SiteThemeBindingResp> bindings = new ArrayList<>();
}
