package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class PartnerEditReq {
    private String partnerName;
    private Long logoFileId;
    private String websiteUrl;
    private String partnerType;
    private Date cooperationStartDate;
    private Date cooperationEndDate;
    private Integer sortNo;
    private String remark;
}
