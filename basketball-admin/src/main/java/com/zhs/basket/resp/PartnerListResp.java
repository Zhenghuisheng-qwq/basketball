package com.zhs.basket.resp;

import lombok.Data;

import java.math.BigDecimal;
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
    /** 累计赞助金额（SUM support_record.support_amount，CASH） */
    private BigDecimal totalSupportAmount;
    /** 赞助次数 */
    private Integer supportCount;
    /** 最近赞助日期 */
    private Date latestSupportTime;
}
