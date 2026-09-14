package com.zhs.basket.support.v4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class V4PublicReadCachePublisher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void matchMutation(Long divisionId, Long matchId, Long seasonId, Long stageId,
                              Long homeEventTeamId, Long awayEventTeamId, Long matchSeriesId) {
        publish(new V4PublicReadCacheEvictEvent(
                divisionId, matchId, seasonId, stageId, homeEventTeamId, awayEventTeamId, matchSeriesId));
    }

    public void standingMutation(Long divisionId, Long seasonId, Long stageId) {
        publish(new V4PublicReadCacheEvictEvent(divisionId, null, seasonId, stageId, null, null));
    }

    public void scheduleMutation(Long divisionId, Long seasonId, Long stageId) {
        publish(new V4PublicReadCacheEvictEvent(divisionId, null, seasonId, stageId, null, null));
    }

    public void seriesMutation(Long divisionId, Long seasonId, Long stageId, Long matchSeriesId,
                               Long homeEventTeamId, Long awayEventTeamId) {
        publish(new V4PublicReadCacheEvictEvent(
                divisionId, null, seasonId, stageId, homeEventTeamId, awayEventTeamId, matchSeriesId));
    }

    public void divisionMutation(Long divisionId, Long seasonId) {
        publish(new V4PublicReadCacheEvictEvent(divisionId, null, seasonId, null, null, null));
    }

    public void seasonMutation(Long seasonId) {
        publish(new V4PublicReadCacheEvictEvent(null, null, seasonId, null, null, null, null, null, true));
    }

    public void teamMutation(Long eventTeamId, Long divisionId, Long seasonId) {
        publish(new V4PublicReadCacheEvictEvent(divisionId, null, seasonId, null, eventTeamId, null));
    }

    public void playerMutation(Long playerId) {
        publish(new V4PublicReadCacheEvictEvent(null, null, null, null, null, null, null, playerId, false));
    }

    public void publish(V4PublicReadCacheEvictEvent event) {
        if (event != null) {
            eventPublisher.publishEvent(event);
        }
    }
}