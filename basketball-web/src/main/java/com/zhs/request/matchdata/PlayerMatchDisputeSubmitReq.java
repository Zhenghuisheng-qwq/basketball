package com.zhs.request.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("比赛数据争议提交请求")
public class PlayerMatchDisputeSubmitReq implements Serializable {

    private String disputeType;
    private String disputeReason;
    private List<String> materialFileIds;
    private Integer expectedHomeScore;
    private Integer expectedAwayScore;
    private String remark;
}
