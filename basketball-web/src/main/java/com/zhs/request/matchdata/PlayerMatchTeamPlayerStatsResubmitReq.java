package com.zhs.request.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("本队球员比赛数据重新提交请求")
public class PlayerMatchTeamPlayerStatsResubmitReq extends PlayerMatchTeamPlayerStatsSubmitReq {
}
