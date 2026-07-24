package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerProfile;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketPlayerProfileMapper {
    BasketPlayerProfile selectByMemberId(Long memberId);
    BasketPlayerProfile selectById(Long id);
    int insert(BasketPlayerProfile profile);
    int updateById(BasketPlayerProfile profile);
    int countByPlayerNo(@Param("playerNo") String playerNo);
    List<BasketPlayerProfile> selectEligibleForCaptain();
}
