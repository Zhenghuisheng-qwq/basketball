package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开首页运营位横幅")
public class PublicHomeBannerResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("横幅主键")
    private Long bannerId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("英文标题")
    private String titleEn;

    @ApiModelProperty("副标题")
    private String subtitle;

    @ApiModelProperty("英文副标题")
    private String subtitleEn;

    @ApiModelProperty("图片 URL")
    private String imageUrl;

    @ApiModelProperty("手机端图片 URL，空则使用电脑端图片")
    private String mobileImageUrl;

    @ApiModelProperty("图片替代文字")
    private String altText;

    @ApiModelProperty("展示位置")
    private String positionCode;

    @ApiModelProperty("跳转类型")
    private String targetType;

    @ApiModelProperty("跳转值")
    private String targetValue;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
