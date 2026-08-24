package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.query.DisciplineQuery;
import com.zhs.basket.vo.DisciplineDetailVo;
import com.zhs.basket.vo.DisciplineListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketDisciplineRecordMapper {
    List<BasketDisciplineRecord> selectActiveBySeasonAndMembers(@Param("seasonId") Long seasonId,
            @Param("memberIds") List<Long> memberIds, @Param("now") Date now);

    List<BasketDisciplineRecord> selectActiveBySubject(@Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId, @Param("seasonId") Long seasonId);

    List<BasketDisciplineRecord> selectActiveBySubjectForUpdate(@Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId, @Param("seasonId") Long seasonId);

    List<BasketDisciplineRecord> selectActivePointDeductionsByEventTeamIds(
            @Param("eventTeamIds") List<Long> eventTeamIds);

    int insert(BasketDisciplineRecord row);

    BasketDisciplineRecord selectById(Long id);

    DisciplineDetailVo selectDetailById(Long id);

    List<DisciplineListVo> selectList(DisciplineQuery query);

    int countByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int updateRecordStatusIfMatch(@Param("id") Long id, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("disciplineStatus") Integer disciplineStatus,
            @Param("operatorId") Long operatorId, @Param("revokeReason") String revokeReason);

    int updateMatchSuspensionRemaining(@Param("id") Long id, @Param("remaining") Integer remaining);

    int updateExecution(@Param("id") Long id,
            @Param("executionStatus") String executionStatus,
            @Param("executedTime") Date executedTime,
            @Param("executionError") String executionError,
            @Param("matchForfeitCount") Integer matchForfeitCount,
            @Param("seriesForfeitCount") Integer seriesForfeitCount,
            @Param("qualificationRemovedCount") Integer qualificationRemovedCount,
            @Param("warningCount") Integer warningCount);

    int bindEventTeamId(@Param("id") Long id, @Param("eventTeamId") Long eventTeamId);

    int updateAnnouncementId(@Param("id") Long id, @Param("announcementId") Long announcementId);

    BasketDisciplineRecord selectActiveMatchSuspension(@Param("seasonId") Long seasonId,
            @Param("memberId") Long memberId, @Param("now") Date now);
}
