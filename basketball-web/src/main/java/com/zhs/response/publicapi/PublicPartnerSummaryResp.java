package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("合作伙伴公开汇总")
public class PublicPartnerSummaryResp implements Serializable {

    @ApiModelProperty("公开展示的合作伙伴总数")
    private Long supportRecordCount;

    @ApiModelProperty("公开合作伙伴累计现金赞助金额")
    private BigDecimal totalCashAmount;
}
