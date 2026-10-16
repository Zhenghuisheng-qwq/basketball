package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistrationRosterChange;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChangePlayer;
import com.zhs.basket.vo.v4.BasketRosterChangeListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketRegistrationRosterChangeMapper {
    BasketRegistrationRosterChange selectById(@Param("id") Long id);

    List<BasketRosterChangeListVo> selectPendingByDivisionId(@Param("divisionId") Long divisionId);

    List<Long> selectPendingIdsByRegistrationId(@Param("registrationId") Long registrationId);

    List<BasketRegistrationRosterChangePlayer> selectPlayers(@Param("changeId") Long changeId);

    int markApproved(@Param("id") Long id, @Param("auditUserId") Long auditUserId, @Param("auditTime") Date auditTime);

    int markRejected(@Param("id") Long id, @Param("auditUserId") Long auditUserId,
                     @Param("auditRemark") String auditRemark, @Param("auditTime") Date auditTime);
}
