package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开社群与媒体入口")
public class PublicSocialLinkResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("平台编码")
    private String platformCode;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("英文标题")
    private String titleEn;

    @ApiModelProperty("描述")
    private String subtitle;

    @ApiModelProperty("英文描述")
    private String subtitleEn;

    @ApiModelProperty("按钮文案")
    private String buttonText;

    @ApiModelProperty("英文按钮文案")
    private String buttonTextEn;

    @ApiModelProperty("跳转链接")
    private String jumpUrl;

    @ApiModelProperty("排序")
    private Integer sortOrder;
}
