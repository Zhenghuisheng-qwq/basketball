package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.exception.ServiceException;
import com.zhs.response.matchdata.PlayerMatchTaskResp;
import com.zhs.response.participation.EventRosterMemberResp;
import com.zhs.response.participation.EventTeamDetailResp;
import com.zhs.response.participation.EventTeamSummaryResp;
import com.zhs.response.participation.RosterChangeHistoryItemResp;
import com.zhs.request.matchdata.PlayerMyScheduleQuery;
import com.zhs.response.matchdata.PlayerMyScheduleEventOptionResp;
import com.zhs.response.matchdata.PlayerMySchedulePageResp;
import com.zhs.response.matchdata.PlayerMySchedulePlatformOptionResp;
import com.zhs.service.IEventParticipationQueryService;
import com.zhs.service.IMPlayerMatchDataService;
import com.zhs.service.IPlayerMyScheduleService;
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

    @Resource
    private IPlayerMyScheduleService playerMyScheduleService;

    @Resource
    private IMPlayerMatchDataService playerMatchDataService;

    @ApiOperation("我的赛程（跨赛事聚合）")
    @GetMapping("/basket/player/my-schedules")
    public R<PlayerMySchedulePageResp> pageMySchedules(@RequestParam(required = false) Long seasonId,
                                                       @RequestParam(required = false) String platformCode,
                                                       @RequestParam(required = false) String matchStatusGroup,
                                                       @RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate,
                                                       @RequestParam(required = false) String sortOrder,
                                                       @RequestParam(required = false) Integer pageNum,
                                                       @RequestParam(required = false) Integer pageSize) {
        PlayerMyScheduleQuery query = new PlayerMyScheduleQuery();
        query.setSeasonId(seasonId);
        query.setPlatformCode(platformCode);
        query.setMatchStatusGroup(matchStatusGroup);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortOrder(sortOrder);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return R.ok(playerMyScheduleService.pageMySchedules(query));
    }

    @ApiOperation("我的赛程赛事筛选项")
    @GetMapping("/basket/player/my-schedules/event-options")
    public R<List<PlayerMyScheduleEventOptionResp>> listMyScheduleEventOptions() {
        return R.ok(playerMyScheduleService.listEventOptions());
    }

    @ApiOperation("我的赛程平台筛选项")
    @GetMapping("/basket/player/my-schedules/platform-options")
    public R<List<PlayerMySchedulePlatformOptionResp>> listMySchedulePlatformOptions() {
        return R.ok(playerMyScheduleService.listPlatformOptions());
    }

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

    @ApiOperation("赛事球队比赛详情")
    @GetMapping("/basket/player/event-team/{eventTeamId}/matches/{matchId}")
    public R<PlayerMatchTaskResp> getEventTeamMatchDetail(@PathVariable String eventTeamId,
                                                          @PathVariable String matchId) {
        return R.ok(playerMatchDataService.getEventTeamMatchDetail(eventTeamId, parseMatchId(matchId)));
    }

    @ApiOperation("赛事球队已发布比赛列表")
    @GetMapping("/basket/player/event-team/{eventTeamId}/matches")
    public R<IPage<PlayerMatchTaskResp>> getEventTeamMatches(@PathVariable String eventTeamId,
                                                             @RequestParam(required = false) Long stageId,
                                                             @RequestParam(required = false) Integer dataStatus,
                                                             @RequestParam(required = false) Integer matchStatus,
                                                             @RequestParam(required = false) String beginTime,
                                                             @RequestParam(required = false) String endTime,
                                                             @RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize) {
        return R.ok(playerMatchDataService.pageEventTeamMatches(eventTeamId, stageId, dataStatus, matchStatus,
                beginTime, endTime, pageNum, pageSize));
    }

    private Long parseMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new ServiceException("比赛主键不能为空");
        }
        try {
            return Long.parseLong(matchId.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException("比赛主键格式不正确");
        }
    }
}
