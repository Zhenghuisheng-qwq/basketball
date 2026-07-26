package com.zhs.response.team;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("可加入球队分页")
public class JoinableTeamPageResp extends Page<TeamSummaryResp> {

    @ApiModelProperty("列表为空时的汇总原因")
    private String emptyReason;

    public JoinableTeamPageResp() {
    }

    public JoinableTeamPageResp(long current, long size, long total) {
        super(current, size, total);
    }
}
