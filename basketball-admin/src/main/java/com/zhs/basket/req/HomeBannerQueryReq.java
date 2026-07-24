package com.zhs.basket.req;

import lombok.Data;

@Data
public class HomeBannerQueryReq {
    private String positionCode;
    private Integer bannerStatus;
    private String keyword;
}
