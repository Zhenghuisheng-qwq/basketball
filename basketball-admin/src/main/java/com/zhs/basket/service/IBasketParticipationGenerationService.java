package com.zhs.basket.service;

public interface IBasketParticipationGenerationService {
    Long generateFromTeamRegistration(Long registrationId, Long auditorId, String remark);

    /** 将已通过报名的正式赛事球队阵容锁定（幂等） */
    void ensureRosterLockedForApprovedRegistration(Long registrationId, Long operatorId);

    /** 对已通过但缺少快照的报名做幂等补偿生成 */
    Long backfillFromApprovedRegistration(Long registrationId, Long operatorId, String remark);
}
