package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketLeaderboardEntry;
import com.zhs.basket.domain.v4.BasketLeaderboardPolicy;
import com.zhs.basket.domain.v4.BasketLeaderboardPolicyAudit;
import com.zhs.basket.domain.v4.BasketLeaderboardRun;
import com.zhs.basket.domain.v4.BasketLeaderboardSetting;
import com.zhs.basket.domain.v4.BasketLeaderboardSnapshot;
import com.zhs.basket.support.v4.leaderboard.LeaderboardFacts;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketLeaderboardMapper {

    BasketLeaderboardPolicy selectPolicyById(@Param("id") Long id);

    BasketLeaderboardPolicy selectOpenDraft(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey);

    BasketLeaderboardPolicy selectActive(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey);

    List<BasketLeaderboardPolicy> selectPolicies(@Param("divisionId") Long divisionId);

    Integer selectMaxVersion(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey);

    int insertPolicy(BasketLeaderboardPolicy policy);

    int updateDraft(BasketLeaderboardPolicy policy);

    int updateLifecycle(BasketLeaderboardPolicy policy);

    int clearActive(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey, @Param("keepId") Long keepId);

    int insertAudit(BasketLeaderboardPolicyAudit audit);

    List<BasketLeaderboardPolicyAudit> selectAudits(@Param("divisionId") Long divisionId);

    int insertRun(BasketLeaderboardRun run);

    int finishRun(BasketLeaderboardRun run);

    List<BasketLeaderboardRun> selectRuns(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey);

    BasketLeaderboardRun selectLatestSuccess(@Param("policyId") Long policyId, @Param("sourceRevision") String sourceRevision);

    int insertSnapshot(BasketLeaderboardSnapshot snapshot);

    int clearPublic(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey);

    int markPublic(@Param("id") Long id, @Param("publicScopeKey") String publicScopeKey, @Param("publishedAt") Date publishedAt);

    BasketLeaderboardSnapshot selectPublicSnapshot(@Param("divisionId") Long divisionId, @Param("scopeKey") String scopeKey);

    int insertEntries(@Param("rows") List<BasketLeaderboardEntry> rows);

    int bindSnapshot(@Param("runId") Long runId, @Param("snapshotId") Long snapshotId);

    List<BasketLeaderboardEntry> selectEntriesByRun(@Param("runId") Long runId, @Param("subjectType") String subjectType);

    List<BasketLeaderboardEntry> selectEntriesBySnapshot(@Param("snapshotId") Long snapshotId, @Param("subjectType") String subjectType);

    List<LeaderboardFacts.MatchFact> selectPublishedMatches(@Param("divisionId") Long divisionId);

    List<LeaderboardFacts.TeamBox> selectTeamBoxes(@Param("divisionId") Long divisionId);

    List<LeaderboardFacts.PlayerBox> selectPlayerBoxes(@Param("divisionId") Long divisionId);

    int countTeamStats(@Param("divisionId") Long divisionId);

    Date maxTeamStatUpdateTime(@Param("divisionId") Long divisionId);

    BasketLeaderboardSetting selectSetting();

    int updateFeatured(@Param("divisionId") Long divisionId, @Param("updateBy") Long updateBy, @Param("updateTime") Date updateTime);
}
