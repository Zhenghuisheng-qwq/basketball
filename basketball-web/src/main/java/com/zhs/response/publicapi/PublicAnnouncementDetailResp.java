package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("公开公告详情")
public class PublicAnnouncementDetailResp extends PublicAnnouncementItemResp {

    @ApiModelProperty("正文纯文本（前端勿当 HTML 渲染）")
    private String contentText;

    @ApiModelProperty("英文正文")
    private String contentTextEn;
}
