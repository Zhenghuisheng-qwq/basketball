package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketMemberProfileChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BasketMemberProfileChangeDao extends BaseMapper<BasketMemberProfileChange> {

    @Select("""
            select *
            from basket_member_profile_change
            where member_id = #{memberId}
              and active_flag = 1
              and is_delete = 0
            limit 1
            """)
    BasketMemberProfileChange selectActiveByMemberId(@Param("memberId") Long memberId);

    @Select("""
            select *
            from basket_member_profile_change
            where member_id = #{memberId}
              and is_delete = 0
            order by id desc
            limit 1
            """)
    BasketMemberProfileChange selectLatestByMemberId(@Param("memberId") Long memberId);
}
