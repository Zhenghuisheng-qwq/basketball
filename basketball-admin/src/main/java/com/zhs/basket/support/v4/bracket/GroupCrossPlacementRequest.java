package com.zhs.basket.support.v4.bracket;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupCrossPlacementRequest {

    Long sourceStageId;
    int advancePerGroup;
    List<GroupLayout> groups;
}
