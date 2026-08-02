package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("合作伙伴公开分页")
public class PublicPartnerPageResp implements Serializable {

    @ApiModelProperty("总数")
    private Long total;

    @ApiModelProperty("记录列表")
    private List<PublicPartnerSupportRecordResp> rows;
}
