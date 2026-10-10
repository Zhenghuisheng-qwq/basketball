package com.zhs.basket.resp;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AnnouncementDetailResp extends AnnouncementListResp {
    private String contentText;
    private String contentTextEn;
}
