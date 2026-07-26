package com.zhs.response.team;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("候选球员分页")
public class CandidatePlayerPageResp extends Page<CandidatePlayerItemResp> {

    @ApiModelProperty("列表为空时的汇总原因")
    private String emptyReason;

    public CandidatePlayerPageResp() {
    }

    public CandidatePlayerPageResp(long current, long size, long total) {
        super(current, size, total);
    }
}
