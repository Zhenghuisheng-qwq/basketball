package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.vo.EligibleCaptainVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketPlayerProfileMapper {
    BasketPlayerProfile selectByMemberId(Long memberId);
    BasketPlayerProfile selectById(Long id);
    int insert(BasketPlayerProfile profile);
    int updateById(BasketPlayerProfile profile);
    int countByPlayerNo(@Param("playerNo") String playerNo);
    Integer selectMaxPlayerNoSequence();
    List<EligibleCaptainVo> selectEligibleCaptainList();

    Long selectAvatarFileIdByMemberId(@Param("memberId") Long memberId);

    int clearCurrentTeamIdByTeamIds(@Param("teamIds") java.util.List<Long> teamIds);

    int clearCurrentTeamIfMatch(@Param("playerId") Long playerId, @Param("teamId") Long teamId);
}
