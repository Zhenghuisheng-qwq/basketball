package com.zhs.basket.support;

import com.zhs.basket.domain.BasketFileAsset;
import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.ParticipationStatusEnum;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.mapper.BasketTeamSeasonProfileMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScheduleEventTeamSupport {

    @Autowired private BasketTeamSeasonProfileMapper eventTeamMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketFileAssetMapper fileAssetMapper;

    public BasketTournamentSeason requireSeason(Long eventId) {
        BasketTournamentSeason season = seasonMapper.selectById(eventId);
        if (season == null || season.getIsDelete() != null && season.getIsDelete() == 1) {
            throw new ServiceException("赛事不存在");
        }
        return season;
    }

    public String resolvePlatformCode(Long eventId) {
        BasketTournamentSeason season = requireSeason(eventId);
        BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
        if (tournament == null) {
            throw new ServiceException("关联赛事不存在");
        }
        String platformCode = GameTerminalSupport.normalize(tournament.getGameTerminal());
        if (platformCode == null) {
            throw new ServiceException("赛事未配置游戏端");
        }
        return platformCode;
    }

    public List<EventTeamScheduleSnapshot> loadActiveSnapshots(Long eventId) {
        List<BasketTeamSeasonProfile> profiles = eventTeamMapper.selectActiveBySeasonId(eventId);
        Map<Long, String> logoMap = loadLogoUrlMap(profiles);
        List<EventTeamScheduleSnapshot> snapshots = new ArrayList<>(profiles.size());
        for (BasketTeamSeasonProfile profile : profiles) {
            EventTeamScheduleSnapshot snapshot = new EventTeamScheduleSnapshot();
            snapshot.setEventTeamId(profile.getId());
            snapshot.setTeamId(profile.getTeamId());
            snapshot.setTeamName(resolveTeamName(profile));
            snapshot.setLogoUrl(logoMap.get(profile.getLogoFileId()));
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    public BasketTeamSeasonProfile requireActiveEventTeam(Long eventTeamId, Long eventId) {
        BasketTeamSeasonProfile profile = eventTeamMapper.selectById(eventTeamId);
        if (profile == null) {
            throw new ServiceException("赛事球队不存在");
        }
        if (!eventId.equals(profile.getSeasonId())) {
            throw new ServiceException("赛事球队不属于当前赛事");
        }
        if (profile.getParticipationStatus() == null
                || profile.getParticipationStatus() != ParticipationStatusEnum.ACTIVE.getCode()) {
            throw new ServiceException("赛事球队当前不可参赛");
        }
        if (profile.getApprovedTime() == null) {
            throw new ServiceException("赛事球队尚未通过报名审核");
        }
        return profile;
    }

    public EventTeamScheduleSnapshot toSnapshot(BasketTeamSeasonProfile profile) {
        EventTeamScheduleSnapshot snapshot = new EventTeamScheduleSnapshot();
        snapshot.setEventTeamId(profile.getId());
        snapshot.setTeamId(profile.getTeamId());
        snapshot.setTeamName(resolveTeamName(profile));
        if (profile.getLogoFileId() != null) {
            BasketFileAsset asset = fileAssetMapper.selectById(profile.getLogoFileId());
            if (asset != null) {
                snapshot.setLogoUrl(asset.getAccessUrl());
            }
        }
        return snapshot;
    }

    private Map<Long, String> loadLogoUrlMap(List<BasketTeamSeasonProfile> profiles) {
        Map<Long, String> map = new HashMap<>();
        for (BasketTeamSeasonProfile profile : profiles) {
            if (profile.getLogoFileId() == null || map.containsKey(profile.getLogoFileId())) {
                continue;
            }
            BasketFileAsset asset = fileAssetMapper.selectById(profile.getLogoFileId());
            if (asset != null) {
                map.put(profile.getLogoFileId(), asset.getAccessUrl());
            }
        }
        return map;
    }

    private String resolveTeamName(BasketTeamSeasonProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        return profile.getTeamNameSnapshot();
    }
}
