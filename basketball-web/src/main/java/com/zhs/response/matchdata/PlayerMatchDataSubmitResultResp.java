package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("比赛数据提交结果")
public class PlayerMatchDataSubmitResultResp implements Serializable {

    private String submissionId;
    private String submissionNo;
    private Integer versionNo;
}
