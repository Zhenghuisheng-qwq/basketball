package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class PartnerAddReq {
    private String partnerName;
    private Long logoFileId;
    private String websiteUrl;
    private String publicDescription;
    private String partnerType;
    private Date cooperationStartDate;
    private Date cooperationEndDate;
    private Integer sortNo;
    private String remark;
    /** 展示状态：1=展示中（默认），0=草稿/不展示，2=已停用 */
    private Integer partnerStatus;
}
