package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchOfficialResult;
import com.zhs.basket.enums.MatchOfficialResultTypeEnum;
import com.zhs.basket.mapper.BasketMatchOfficialResultMapper;
import com.zhs.basket.service.IBasketMatchOfficialResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BasketMatchOfficialResultServiceImpl implements IBasketMatchOfficialResultService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Autowired
    private BasketMatchOfficialResultMapper officialResultMapper;

    @Override
    public BasketMatchOfficialResult getCurrent(Long matchId) {
        return officialResultMapper.selectCurrentByMatchId(matchId);
    }

    @Override
    public BasketMatchOfficialResult publishFromMatch(BasketMatch match, Long auditorId) {
        officialResultMapper.supersedeCurrent(match.getId());
        Date now = new Date();
        BasketMatchOfficialResult current = officialResultMapper.selectCurrentByMatchId(match.getId());
        int revisionNo = current != null && current.getRevisionNo() != null ? current.getRevisionNo() + 1 : 1;
        int dataVersion = match.getDataVersion() != null ? match.getDataVersion() : 1;
        Long loserId = null;
        if (match.getWinnerTeamId() != null) {
            loserId = match.getWinnerTeamId().equals(match.getHomeTeamId())
                    ? match.getAwayTeamId() : match.getHomeTeamId();
        }
        String resultType = match.getWalkoverTeamId() != null
                ? MatchOfficialResultTypeEnum.FORFEIT.getCode()
                : MatchOfficialResultTypeEnum.NORMAL.getCode();
        int isDraw = nvl(match.getHomeScore()) == nvl(match.getAwayScore()) ? 1 : 0;

        BasketMatchOfficialResult row = new BasketMatchOfficialResult();
        row.setMatchId(match.getId());
        row.setDataVersion(dataVersion);
        row.setHomeScore(nvl(match.getHomeScore()));
        row.setAwayScore(nvl(match.getAwayScore()));
        row.setWinnerTeamId(match.getWinnerTeamId());
        row.setLoserTeamId(loserId);
        row.setIsDraw(isDraw);
        row.setResultType(resultType);
        row.setEffectiveTime(match.getActualEndTime() != null ? match.getActualEndTime() : now);
        row.setConfirmedBy(auditorId);
        row.setConfirmedTime(now);
        row.setIsCurrent(IS_CURRENT);
        row.setIsPublic(match.getPublicStatus() != null ? match.getPublicStatus() : 1);
        row.setRevisionNo(revisionNo);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        officialResultMapper.insert(row);
        return row;
    }

    @Override
    public void supersedeCurrent(Long matchId) {
        officialResultMapper.supersedeCurrent(matchId);
    }

    private int nvl(Integer v) {
        return v != null ? v : 0;
    }
}
