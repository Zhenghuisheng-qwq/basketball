package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("公开合作伙伴支持记录")
public class PublicPartnerSupportRecordResp implements Serializable {

    @ApiModelProperty("支持记录ID")
    private Long recordId;

    @ApiModelProperty("合作伙伴ID")
    private Long partnerId;

    @ApiModelProperty("合作伙伴名称")
    private String partnerName;

    @ApiModelProperty("Logo URL")
    private String partnerLogoUrl;

    @ApiModelProperty("官网链接")
    private String websiteUrl;

    @ApiModelProperty("支持方式")
    private String supportType;

    @ApiModelProperty("支持方式中文")
    private String supportTypeText;

    @ApiModelProperty("支持金额")
    private BigDecimal supportAmount;

    @ApiModelProperty("展示文案")
    private String supportDisplayText;

    @ApiModelProperty("支持日期")
    private String supportDate;

    @ApiModelProperty("公开说明")
    private String publicDescription;
}
