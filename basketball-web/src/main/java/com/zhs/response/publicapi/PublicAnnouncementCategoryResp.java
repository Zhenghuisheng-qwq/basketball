package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("公开公告分类")
public class PublicAnnouncementCategoryResp implements Serializable {

    @ApiModelProperty("分类编码")
    private String categoryCode;

    @ApiModelProperty("分类名称")
    private String categoryName;
}
