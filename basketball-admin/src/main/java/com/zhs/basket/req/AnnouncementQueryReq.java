package com.zhs.basket.req;

import lombok.Data;

@Data
public class AnnouncementQueryReq {
    private String categoryCode;
    private Integer announcementStatus;
    private Integer isTop;
    private Integer isImportant;
    private String keyword;
}
