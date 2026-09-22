package com.zhs.basket.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PartnerSupportListResp {
    private Long id;
    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private String partnerLogoUrl;
    private String supportType;
    private String supportTypeName;
    private BigDecimal supportAmount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date supportDate;
    private String publicDescription;
    private Integer publishStatus;
    private String publishStatusName;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
