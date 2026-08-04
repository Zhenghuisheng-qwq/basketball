package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.GroupingStatusEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupStageScheduleStrategyTest {

    private final GroupStageStageScheduleStrategy strategy = new GroupStageStageScheduleStrategy();

    @Test
    void preview_sixteenTeamsFourGroups_twentyFourMatches() {
        StageExecutionContext context = buildGroupContext(16, 4);
        StageSchedulePreviewVo preview = strategy.preview(context);
        assertEquals(16, preview.getTeamCount());
        assertEquals(24, preview.getTotalMatches());
        assertEquals(4, preview.getGroupPreviews().size());
        for (StageSchedulePreviewVo.GroupPreview gp : preview.getGroupPreviews()) {
            assertEquals(4, gp.getTeamCount());
            assertEquals(6, gp.getTotalMatches());
        }
    }

    private StageExecutionContext buildGroupContext(int totalTeams, int groupCount) {
        BasketDivision division = new BasketDivision();
        division.setId(200L);
        division.setSeasonId(1L);

        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(20L);
        stage.setDivisionId(200L);
        stage.setStageType(StageTypeEnum.GROUP.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        stage.setGroupCount(groupCount);
        stage.setBestOf(1);

        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 1; i <= totalTeams; i++) {
            BasketDivisionStageTeam t = new BasketDivisionStageTeam();
            t.setEventTeamId((long) i);
            teams.add(t);
        }

        StageExecutionContext ctx = new StageExecutionContext();
        ctx.setDivision(division);
        ctx.setStage(stage);
        ctx.setStageTeams(teams);
        ctx.setExecutionVersion(1);

        List<com.zhs.basket.domain.v4.BasketDivisionGroup> groups = new ArrayList<>();
        List<com.zhs.basket.domain.v4.BasketDivisionGroupTeam> groupTeams = new ArrayList<>();
        int perGroup = totalTeams / groupCount;
        for (int g = 0; g < groupCount; g++) {
            com.zhs.basket.domain.v4.BasketDivisionGroup group = new com.zhs.basket.domain.v4.BasketDivisionGroup();
            group.setId((long) (g + 1));
            group.setGroupCode(String.valueOf((char) ('A' + g)));
            group.setGroupName("Group " + group.getGroupCode());
            group.setQualifyCount(2);
            groups.add(group);
            for (int i = 0; i < perGroup; i++) {
                com.zhs.basket.domain.v4.BasketDivisionGroupTeam gt = new com.zhs.basket.domain.v4.BasketDivisionGroupTeam();
                gt.setGroupId(group.getId());
                gt.setEventTeamId((long) (g * perGroup + i + 1));
                groupTeams.add(gt);
            }
        }
        ctx.setGroups(groups);
        ctx.setGroupTeams(groupTeams);
        BasketDivisionGroupingBatch batch = new BasketDivisionGroupingBatch();
        batch.setId(1L);
        batch.setStageId(stage.getId());
        batch.setGroupingStatus(GroupingStatusEnum.LOCKED.getCode());
        batch.setGroupCount(groupCount);
        ctx.setGroupingBatch(batch);
        return ctx;
    }
}
