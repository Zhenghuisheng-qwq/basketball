package com.zhs.basket.req;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PartnerSupportAddReq {
    private Long partnerId;
    /** 合作伙伴名称；与 partnerId 二选一，优先 partnerId */
    private String partnerName;
    private String supportType;
    private BigDecimal supportAmount;
    private Date supportDate;
    private String publicDescription;
    private Integer publishStatus;
    private Integer sortOrder;
}
