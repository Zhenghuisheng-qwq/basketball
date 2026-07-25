package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerPlatformChangeRequest;
import com.zhs.basket.query.PlayerPlatformChangeQuery;
import com.zhs.basket.vo.PlayerPlatformChangeDetailVo;
import com.zhs.basket.vo.PlayerPlatformChangeListVo;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketPlayerPlatformChangeRequestMapper {
    List<PlayerPlatformChangeListVo> selectList(PlayerPlatformChangeQuery query);
    PlayerPlatformChangeDetailVo selectDetailVo(Long id);
    BasketPlayerPlatformChangeRequest selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("reviewerId") Long reviewerId,
            @Param("reviewRemark") String reviewRemark, @Param("reviewedTime") Date reviewedTime,
            @Param("resultPlatformAccountId") Long resultPlatformAccountId);
}
