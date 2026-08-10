package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class PartnerSupportQueryReq {
    private Long partnerId;
    private String partnerName;
    private String supportType;
    private Integer publishStatus;
    private Date supportDateBegin;
    private Date supportDateEnd;
}
