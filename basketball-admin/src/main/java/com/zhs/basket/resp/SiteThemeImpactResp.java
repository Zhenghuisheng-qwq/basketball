package com.zhs.basket.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SiteThemeImpactResp {
    private List<String> affected = new ArrayList<>();
    private String replaceHint;
}
