package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerCertApplication;
import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.vo.PlayerCertDetailVo;
import com.zhs.basket.vo.PlayerCertListVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketPlayerCertApplicationMapper {
    List<PlayerCertListVo> selectList(PlayerCertQuery query);
    PlayerCertDetailVo selectDetailVo(Long id);
    BasketPlayerCertApplication selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("currentAuditorId") Long currentAuditorId,
            @Param("reviewRemark") String reviewRemark, @Param("reviewedTime") Date reviewedTime,
            @Param("auditorId") Long auditorId);
    int updatePlayerIdAndNormalized(@Param("id") Long id, @Param("playerId") Long playerId,
            @Param("normalizedGameAccount") String normalizedGameAccount);
    List<BasketPlayerCertApplication> selectByMemberId(@Param("memberId") Long memberId);
}
