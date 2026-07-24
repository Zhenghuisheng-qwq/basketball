package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("公开荣誉详情（含阵容）")
public class PublicHonorDetailResp extends PublicHonorItemResp {

    @ApiModelProperty("冠军阵容")
    private List<PublicHonorLineupItemResp> lineup;
}
