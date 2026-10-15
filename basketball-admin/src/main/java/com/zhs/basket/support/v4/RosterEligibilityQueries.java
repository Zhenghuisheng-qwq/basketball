package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RosterEligibilityQueries {

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;

    public boolean frozen(Long divisionId) {
        if (divisionId == null) {
            return false;
        }
        BasketDivision division = divisionMapper.selectById(divisionId);
        String status = division == null ? null : division.getDivisionStatus();
        int started = matchMapper.countStartedByDivisionId(divisionId);
        int stats = matchPlayerStatMapper.countByDivisionId(divisionId)
                + submissionPlayerStatMapper.countByDivisionId(divisionId);
        return RosterChangeWindowSupport.isRosterFrozen(status, started, stats);
    }

    public int eligibleCount(Long registrationId, Long divisionId) {
        if (registrationId == null) {
            return 0;
        }
        if (frozen(divisionId)) {
            return registrationPlayerMapper.countRosterPlayers(registrationId);
        }
        return registrationPlayerMapper.countCurrentEligible(registrationId);
    }
}
