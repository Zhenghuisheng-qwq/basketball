package com.zhs.support.v4;

import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.v4.BasketEventTeam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventTeamParticipationGuard {

    public static final String MSG_DISQUALIFIED = "\u8be5\u8d5b\u4e8b\u7403\u961f\u5df2\u88ab\u53d6\u6d88\u8d5b\u4e8b\u8d44\u683c\uff0c\u4e0d\u80fd\u7ee7\u7eed\u53c2\u8d5b";
    public static final String STATUS_DISQUALIFIED = "DISQUALIFIED";

    @Autowired
    private BasketEventTeamDao eventTeamDao;

    public boolean isDisqualified(Long eventTeamId) {
        if (eventTeamId == null || eventTeamId <= 0) {
            return false;
        }
        BasketEventTeam team = eventTeamDao.selectById(eventTeamId);
        return team != null && STATUS_DISQUALIFIED.equalsIgnoreCase(team.getEventTeamStatus());
    }

    public void assertCanParticipate(Long eventTeamId) {
        if (isDisqualified(eventTeamId)) {
            throw new ServiceException(MSG_DISQUALIFIED);
        }
    }
}