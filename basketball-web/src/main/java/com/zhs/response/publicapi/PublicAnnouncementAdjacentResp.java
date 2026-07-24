package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开公告上一篇/下一篇")
public class PublicAnnouncementAdjacentResp implements Serializable {

    @ApiModelProperty("上一篇（更早发布，可为 null）")
    private PublicAnnouncementItemResp previous;

    @ApiModelProperty("下一篇（更新发布，可为 null）")
    private PublicAnnouncementItemResp next;
}
