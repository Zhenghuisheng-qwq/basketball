package com.zhs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhs.model.BasketTeamProfileChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface BasketTeamProfileChangeDao extends BaseMapper<BasketTeamProfileChange> {

    @Select("""
            select *
            from basket_team_profile_change
            where team_id = #{teamId}
              and active_flag = 1
              and is_delete = 0
            limit 1
            """)
    BasketTeamProfileChange selectActiveByTeamId(@Param("teamId") Long teamId);

    @Update("""
            <script>
            update basket_team_profile_change
            set audit_status = #{toStatus},
                audit_user_id = #{auditUserId},
                audit_remark = #{auditRemark},
                audit_time = #{auditTime},
                active_flag = #{activeFlag},
                update_time = now()
            where id = #{id}
              and is_delete = 0
              and audit_status in
            <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{s}</foreach>
            </script>
            """)
    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatuses") List<Integer> fromStatuses,
                            @Param("toStatus") Integer toStatus,
                            @Param("auditUserId") Long auditUserId,
                            @Param("auditRemark") String auditRemark,
                            @Param("auditTime") Date auditTime,
                            @Param("activeFlag") Integer activeFlag);

    @Update("""
            <script>
            update basket_team_profile_change
            set audit_status = #{toStatus},
                cancel_reason = #{cancelReason},
                cancel_time = #{cancelTime},
                active_flag = 0,
                update_time = now()
            where id = #{id}
              and team_id = #{teamId}
              and is_delete = 0
              and audit_status in
            <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{s}</foreach>
            </script>
            """)
    int cancelIfMatch(@Param("id") Long id,
                      @Param("teamId") Long teamId,
                      @Param("fromStatuses") List<Integer> fromStatuses,
                      @Param("toStatus") Integer toStatus,
                      @Param("cancelReason") String cancelReason,
                      @Param("cancelTime") Date cancelTime);
}
