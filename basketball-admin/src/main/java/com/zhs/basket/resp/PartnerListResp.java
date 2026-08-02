package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class PartnerListResp {
    private Long id;
    private String partnerCode;
    private String partnerName;
    private Long logoFileId;
    private String logoUrl;
    private String websiteUrl;
    private String publicDescription;
    private String partnerType;
    private Date cooperationStartDate;
    private Date cooperationEndDate;
    private Integer partnerStatus;
    private String partnerStatusName;
    private Integer sortNo;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
