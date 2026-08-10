package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMemberProfileChange;
import com.zhs.basket.query.MemberProfileChangeQuery;
import com.zhs.basket.vo.MemberProfileChangeDetailVo;
import com.zhs.basket.vo.MemberProfileChangeListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketMemberProfileChangeMapper {

    List<MemberProfileChangeListVo> selectList(MemberProfileChangeQuery query);

    MemberProfileChangeDetailVo selectDetailVo(@Param("id") Long id);

    BasketMemberProfileChange selectById(@Param("id") Long id);

    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatuses") List<Integer> fromStatuses,
                            @Param("toStatus") Integer toStatus,
                            @Param("auditUserId") Long auditUserId,
                            @Param("auditRemark") String auditRemark,
                            @Param("auditTime") Date auditTime,
                            @Param("activeFlag") Integer activeFlag);

    int updateMemberOfficialProfile(@Param("memberId") Long memberId,
                                    @Param("nickname") String nickname,
                                    @Param("avatarFileId") Long avatarFileId);
}
