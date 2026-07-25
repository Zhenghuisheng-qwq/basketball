package com.zhs.response.team;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("可报名赛事分页")
public class AvailableSeasonPageResp extends Page<AvailableSeasonItemResp> {

    @ApiModelProperty("列表为空时的汇总原因")
    private String emptyReason;

    public AvailableSeasonPageResp() {
    }

    public AvailableSeasonPageResp(long current, long size, long total) {
        super(current, size, total);
    }
}
