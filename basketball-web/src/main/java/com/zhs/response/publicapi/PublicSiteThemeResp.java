package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@ApiModel("公开站点主题")
public class PublicSiteThemeResp implements Serializable {

    @ApiModelProperty("是否有已发布主题。false 时页面使用现在的默认颜色")
    private Boolean configured;

    @ApiModelProperty("实际命中的范围：HOME、SEASON、BASE、DEFAULT")
    private String source;

    @ApiModelProperty("背景色")
    private String bgColor;

    @ApiModelProperty("背景图")
    private String bgImageUrl;

    @ApiModelProperty("手机背景图")
    private String mobileBgImageUrl;

    @ApiModelProperty("TOP_CENTER、SCROLL 或 FIXED")
    private String bgDisplay;

    @ApiModelProperty("语义色")
    private Map<String, String> colors;
}
