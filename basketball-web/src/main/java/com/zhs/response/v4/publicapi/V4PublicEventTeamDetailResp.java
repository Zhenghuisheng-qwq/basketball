package com.zhs.response.v4.publicapi;

import com.zhs.response.publicapi.PublicActiveDisciplineSummaryVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class V4PublicEventTeamDetailResp extends V4PublicEventTeamItemResp {

    private List<V4PublicEventPlayerItemResp> players;

    private String description;

    private List<PublicActiveDisciplineSummaryVo> activeDisciplineSummary;
}
