package com.zhs.basket.support.v4.stage;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶段赛程生成预览。
 */
@Data
public class StageSchedulePreviewVo {

    private Integer teamCount;
    private Integer matchesPerTeam;
    private Integer totalMatches;
    private Integer roundCount;
    private Boolean hasBye;
    private List<String> warnings = new ArrayList<>();
    private List<GroupPreview> groupPreviews = new ArrayList<>();

    @Data
    public static class GroupPreview {
        private Long groupId;
        private String groupCode;
        private String groupName;
        private Integer teamCount;
        private Integer totalMatches;
        private Integer matchesPerTeam;
        private Integer roundCount;
        private Boolean hasBye;
    }
}
