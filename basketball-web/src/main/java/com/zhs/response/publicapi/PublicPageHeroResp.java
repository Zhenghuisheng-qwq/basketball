package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开页面头图")
public class PublicPageHeroResp implements Serializable {

    @ApiModelProperty("是否有可显示的已发布头图")
    private Boolean configured;

    @ApiModelProperty("页面标识")
    private String pageKey;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季 ID，通用页为空")
    private Long seasonId;

    @ApiModelProperty("FULL_IMAGE 或 BACKGROUND_TEXT")
    private String mode;

    @ApiModelProperty("电脑端图片")
    private String imageUrl;

    @ApiModelProperty("手机端图片，空则使用电脑端图片")
    private String mobileImageUrl;

    @ApiModelProperty("水平焦点 0-100")
    private Integer focusX;

    @ApiModelProperty("垂直焦点 0-100")
    private Integer focusY;

    @ApiModelProperty("遮罩 0-100，只作用于头图")
    private Integer maskOpacity;

    @ApiModelProperty("替代文字")
    private String altText;
}
