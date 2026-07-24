package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开合作伙伴")
public class PublicPartnerResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("合作伙伴主键")
    private Long partnerId;

    @ApiModelProperty("名称")
    private String partnerName;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("网站地址")
    private String websiteUrl;

    @ApiModelProperty("合作类型")
    private String partnerType;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
