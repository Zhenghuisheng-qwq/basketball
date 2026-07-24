package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRegistrationPlayer;
import com.zhs.basket.vo.AvailableMatchPlayerVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentRegistrationPlayerMapper {
    List<BasketTournamentRegistrationPlayer> selectByRegistrationId(Long registrationId);
    BasketTournamentRegistrationPlayer selectById(Long id);
    List<AvailableMatchPlayerVo> selectAvailableBySeasonAndTeam(@Param("seasonId") Long seasonId,
            @Param("teamId") Long teamId);
    int updateValidationStatus(@Param("id") Long id, @Param("validationStatus") Integer validationStatus,
            @Param("rejectReason") String rejectReason);
}
