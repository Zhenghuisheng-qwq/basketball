package com.zhs.basket.req;

import lombok.Data;

@Data
public class PartnerQueryReq {
    private String partnerType;
    private Integer partnerStatus;
    private String keyword;
    private String partnerCode;
}
