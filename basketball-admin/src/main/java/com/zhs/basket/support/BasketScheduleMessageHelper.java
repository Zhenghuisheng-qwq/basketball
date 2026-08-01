package com.zhs.basket.support;

import com.zhs.basket.domain.BasketScheduleGenerateBatch;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.mapper.BasketMessageTaskMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class BasketScheduleMessageHelper {

    public static final String BIZ_TYPE_MATCH_SCHEDULE_PUBLISHED = "MATCH_SCHEDULE_PUBLISHED";

    private static final Logger log = LoggerFactory.getLogger(BasketScheduleMessageHelper.class);

    @Autowired
    private BasketAdminMessageService messageService;
    @Autowired
    private BasketTournamentSeasonRosterMapper rosterMapper;
    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;
    @Autowired
    private BasketTournamentMapper tournamentMapper;
    @Autowired
    private BasketTournamentStageMapper stageMapper;
    @Autowired
    private BasketMessageTaskMapper messageTaskMapper;

    public void notifySchedulePublished(BasketScheduleGenerateBatch batch, Long stageId, Long adminUserId) {
        if (batch == null || batch.getId() == null || batch.getEventId() == null) {
            return;
        }
        if (messageTaskMapper.countByBizTypeAndBizId(BIZ_TYPE_MATCH_SCHEDULE_PUBLISHED, batch.getId()) > 0) {
            log.info("赛程批次 {} 已发送过发布通知，跳过重复发送", batch.getId());
            return;
        }

        List<Long> memberIds = rosterMapper.selectActiveMemberIdsBySeasonId(batch.getEventId());
        if (CollectionUtils.isEmpty(memberIds)) {
            log.warn("赛季 {} 无有效阵容成员，跳过赛程发布通知", batch.getEventId());
            return;
        }

        BasketTournamentSeason season = seasonMapper.selectById(batch.getEventId());
        BasketTournament tournament = season == null ? null : tournamentMapper.selectById(season.getTournamentId());
        BasketTournamentStage stage = stageId == null ? null : stageMapper.selectById(stageId);
        String tournamentName = tournament == null ? "赛事" : tournament.getTournamentName();
        String seasonName = season == null ? "" : season.getSeasonName();
        String stageName = stage == null ? "常规赛" : stage.getStageName();
        String platformCode = tournament == null ? null : GameTerminalSupport.normalize(tournament.getGameTerminal());
        String platform = GameTerminalSupport.formatDisplayName(platformCode);

        String title = "赛程已发布";
        String content = String.format("您参加的【%s / %s / %s】赛程已发布，请前往我的赛事查看比赛安排。",
                firstNonBlank(tournamentName, seasonName, "赛事"), stageName, platform);
        messageService.sendSiteMessages(memberIds, adminUserId, BIZ_TYPE_MATCH_SCHEDULE_PUBLISHED, batch.getId(), title, content);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
