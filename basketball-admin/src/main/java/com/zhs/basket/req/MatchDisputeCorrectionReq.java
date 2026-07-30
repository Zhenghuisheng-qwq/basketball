package com.zhs.basket.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MatchDisputeCorrectionReq extends MatchOfficialDataCorrectionReq {
    private String handleRemark;
}
