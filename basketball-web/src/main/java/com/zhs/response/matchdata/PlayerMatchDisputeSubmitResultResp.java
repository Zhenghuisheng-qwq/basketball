package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("比赛数据争议提交结果")
public class PlayerMatchDisputeSubmitResultResp implements Serializable {

    private String disputeId;
    private String disputeNo;
}
