package com.zhs.basket.req;

import lombok.Data;

@Data
public class AnnouncementAddReq {
    private String categoryCode;
    private String title;
    private String summary;
    private Long coverFileId;
    private String contentText;
    private String publicScope;
    private Integer isImportant;
}
