package com.zhs.controller;

import com.zhs.response.participation.EventRosterMemberResp;
import com.zhs.response.participation.EventTeamDetailResp;
import com.zhs.response.participation.EventTeamSummaryResp;
import com.zhs.response.participation.RosterChangeHistoryItemResp;
import com.zhs.service.IEventParticipationQueryService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(value = "赛事参赛查询")
@Slf4j
public class EventParticipationQueryController {

    @Resource
    private IEventParticipationQueryService eventParticipationQueryService;

    @ApiOperation("我的赛事球队列表")
    @GetMapping("/basket/player/event-team/list")
    public R<List<EventTeamSummaryResp>> getMyEventTeams(@RequestParam(required = false) String seasonId) {
        return R.ok(eventParticipationQueryService.getMyEventTeams(seasonId));
    }

    @ApiOperation("赛事球队详情")
    @GetMapping("/basket/player/event-team/{eventTeamId}")
    public R<EventTeamDetailResp> getEventTeamDetail(@PathVariable String eventTeamId) {
        return R.ok(eventParticipationQueryService.getEventTeamDetail(eventTeamId));
    }

    @ApiOperation("赛事球队阵容")
    @GetMapping("/basket/player/event-team/{eventTeamId}/roster")
    public R<List<EventRosterMemberResp>> getEventRoster(@PathVariable String eventTeamId) {
        return R.ok(eventParticipationQueryService.getEventRoster(eventTeamId));
    }

    @ApiOperation("阵容变更历史")
    @GetMapping("/basket/player/event-team/roster-history")
    public R<List<RosterChangeHistoryItemResp>> getRosterHistory(@RequestParam String seasonId,
                                                                 @RequestParam(required = false) String playerId,
                                                                 @RequestParam(required = false) String eventTeamId) {
        return R.ok(eventParticipationQueryService.getRosterHistory(seasonId, playerId, eventTeamId));
    }
}
