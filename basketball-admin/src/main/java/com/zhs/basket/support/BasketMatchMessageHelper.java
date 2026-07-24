package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketTournamentRegistration;
import com.zhs.basket.mapper.BasketTournamentRegistrationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BasketMatchMessageHelper {

    @Autowired
    private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired
    private BasketAdminMessageService messageService;

    public void notifyCaptains(BasketMatch match, Long adminUserId, String title, String content) {
        List<Long> memberIds = new ArrayList<>(2);
        BasketTournamentRegistration home = registrationMapper.selectApprovedBySeasonAndTeam(match.getSeasonId(), match.getHomeTeamId());
        BasketTournamentRegistration away = registrationMapper.selectApprovedBySeasonAndTeam(match.getSeasonId(), match.getAwayTeamId());
        if (home != null && home.getCaptainMemberId() != null) {
            memberIds.add(home.getCaptainMemberId());
        }
        if (away != null && away.getCaptainMemberId() != null) {
            memberIds.add(away.getCaptainMemberId());
        }
        messageService.sendSiteMessages(memberIds, adminUserId, "MATCH", match.getId(), title, content);
    }
}
