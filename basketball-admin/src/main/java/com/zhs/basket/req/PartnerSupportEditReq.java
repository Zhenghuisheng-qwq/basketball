package com.zhs.basket.req;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PartnerSupportEditReq {
    private Long partnerId;
    private String supportType;
    private BigDecimal supportAmount;
    private Date supportDate;
    private String publicDescription;
    private Integer publishStatus;
    private Integer sortOrder;
}
