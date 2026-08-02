package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("合作伙伴公开聚合响应")
public class PublicPartnersResp implements Serializable {

    @ApiModelProperty("汇总")
    private PublicPartnerSummaryResp summary;

    @ApiModelProperty("分页")
    private PublicPartnerPageResp page;
}
