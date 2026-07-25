package com.zhs.basket.localdemo.mapper;

import org.apache.ibatis.annotations.Param;

import com.zhs.basket.localdemo.dto.DemoMemberAccountRow;

import java.util.Date;
import java.util.List;

/**
 * 本地演示数据专用 Mapper，仅用于 LOCAL-DEMO- 标识数据的读写与清理。
 */
public interface BasketLocalDemoDataMapper {

    String PREFIX = "LOCAL-DEMO-";
    String MARKER = "CNPA_LOCAL_DEMO_V1";

    Long selectIdByAccountNo(@Param("accountNo") String accountNo);

    Long selectIdByPlayerNo(@Param("playerNo") String playerNo);

    Long selectIdByTeamNo(@Param("teamNo") String teamNo);

    Long selectIdByTournamentNo(@Param("tournamentNo") String tournamentNo);

    Long selectIdBySeasonNo(@Param("seasonNo") String seasonNo);

    Long selectIdByMatchNo(@Param("matchNo") String matchNo);

    Long selectIdByCertApplicationNo(@Param("applicationNo") String applicationNo);

    Long selectIdByRegistrationNo(@Param("registrationNo") String registrationNo);

    Long selectIdByAnnouncementNo(@Param("announcementNo") String announcementNo);

    Long selectIdByBannerName(@Param("bannerName") String bannerName);

    Long selectIdByStageNo(@Param("stageNo") String stageNo);

    Long selectIdByRoundNo(@Param("seasonNo") String seasonNo, @Param("roundSequence") int roundSequence);

    Long selectIdByGroupCode(@Param("groupCode") String groupCode);

    Long selectPlaceholderFileIdByRemark(@Param("remark") String remark);

    Long selectPlaceholderFileIdByRemarkAny(@Param("remark") String remark);

    int restorePlaceholderFileByRemark(@Param("remark") String remark);

    Long selectDemoSeasonId();

    Long selectRegistrationPlayerId(@Param("registrationId") Long registrationId, @Param("playerId") Long playerId);

    List<Long> selectAccountIdsByPrefix(@Param("prefix") String prefix);

    List<Long> selectTeamIdsByPrefix(@Param("prefix") String prefix);

    List<Long> selectApplicationIdsByPrefix(@Param("prefix") String prefix);

    List<Long> selectRegistrationIdsByPrefix(@Param("prefix") String prefix);

    List<Long> selectMatchIdsByPrefix(@Param("prefix") String prefix);

    int countAccounts(@Param("prefix") String prefix);

    int countAccountsIncludingDeleted(@Param("prefix") String prefix);

    int countDemoMembersMissingAvatar(@Param("prefix") String prefix);

    List<DemoMemberAccountRow> selectDemoMemberAccounts(@Param("prefix") String prefix);

    int updateMemberProfileAvatar(@Param("memberId") Long memberId, @Param("avatarFileId") Long avatarFileId);

    Long selectSeasonIdBySeasonNoForReset(@Param("seasonNo") String seasonNo);

    List<Long> selectGroupIdsByPrefix(@Param("prefix") String prefix);

    Long selectGroupIdByGroupCodeForReset(@Param("groupCode") String groupCode);

    int countPlayers(@Param("prefix") String prefix);

    int countCaptainQualifications(@Param("prefix") String prefix, @Param("marker") String marker);

    int countTeams(@Param("prefix") String prefix);

    int countTeamMembers(@Param("prefix") String prefix);

    int countTournaments(@Param("prefix") String prefix);

    int countSeasons(@Param("prefix") String prefix);

    int countRegistrations(@Param("prefix") String prefix, @Param("marker") String marker);

    int countRegistrationPlayers(@Param("prefix") String prefix);

    int countStages(@Param("prefix") String prefix);

    int countRounds(@Param("prefix") String prefix);

    int countGroups(@Param("prefix") String prefix);

    int countGroupTeams(@Param("prefix") String prefix);

    int countMatches(@Param("prefix") String prefix);

    int countConfirmedMatches(@Param("prefix") String prefix);

    int countScheduledMatches(@Param("prefix") String prefix);

    int countLineups(@Param("prefix") String prefix);

    int countPeriodScores(@Param("prefix") String prefix);

    int countTeamMatchStats(@Param("prefix") String prefix);

    int countPlayerMatchStats(@Param("prefix") String prefix);

    int countStandingsBySeasonId(@Param("seasonId") Long seasonId);

    int countTeamSeasonStatsBySeasonId(@Param("seasonId") Long seasonId);

    int countPlayerSeasonStatsBySeasonId(@Param("seasonId") Long seasonId);

    int countTeamRanks(@Param("seasonId") Long seasonId);

    int countPlayerRanks(@Param("seasonId") Long seasonId);

    int countAnnouncements(@Param("prefix") String prefix);

    int countBanners(@Param("prefix") String prefix);

    int existsNonDemoReference(@Param("prefix") String prefix);

    int insertPlaceholderFileAsset(@Param("objectKey") String objectKey, @Param("remark") String remark);

    int insertDemoImageFileAsset(@Param("objectKey") String objectKey, @Param("fileName") String fileName,
                                 @Param("accessUrl") String accessUrl, @Param("usageType") String usageType,
                                 @Param("bucketName") String bucketName, @Param("remark") String remark);

    Long selectFileIdByObjectKey(@Param("objectKey") String objectKey);

    Long selectFileIdByObjectKeyAny(@Param("objectKey") String objectKey);

    int restoreFileByObjectKey(@Param("objectKey") String objectKey);

    int insertMemberAccount(@Param("accountNo") String accountNo, @Param("passwordHash") String passwordHash,
                            @Param("email") String email,
                            @Param("username") String username, @Param("registerSource") String registerSource,
                            @Param("registerTerminal") String registerTerminal);

    int insertMemberProfile(@Param("memberId") Long memberId, @Param("nickname") String nickname,
                            @Param("avatarFileId") Long avatarFileId);

    int insertPrivacySetting(@Param("memberId") Long memberId);

    int insertNotificationSetting(@Param("memberId") Long memberId);

    int insertCertApplication(@Param("applicationNo") String applicationNo, @Param("memberId") Long memberId,
                              @Param("gameTerminal") String gameTerminal, @Param("gameAccount") String gameAccount,
                              @Param("gameNickname") String gameNickname, @Param("positionCode") String positionCode,
                              @Param("applicationStatus") Integer applicationStatus,
                              @Param("submittedTime") Date submittedTime);

    int insertCertMaterial(@Param("applicationId") Long applicationId, @Param("fileId") Long fileId,
                           @Param("materialType") String materialType, @Param("remark") String remark);

    int insertTeam(@Param("teamNo") String teamNo, @Param("teamName") String teamName,
                   @Param("shortName") String shortName, @Param("englishName") String englishName,
                   @Param("description") String description, @Param("gameTerminal") String gameTerminal,
                   @Param("captainMemberId") Long captainMemberId, @Param("captainPlayerId") Long captainPlayerId,
                   @Param("maxMemberCount") Integer maxMemberCount,
                   @Param("currentMemberCount") Integer currentMemberCount,
                   @Param("auditStatus") Integer auditStatus);

    int insertTeamMember(@Param("membershipNo") String membershipNo, @Param("teamId") Long teamId,
                         @Param("memberId") Long memberId, @Param("playerId") Long playerId,
                         @Param("roleType") String roleType, @Param("jerseyNo") String jerseyNo,
                         @Param("joinSource") String joinSource, @Param("remark") String remark,
                         @Param("joinTime") Date joinTime);

    int insertTeamInvitation(@Param("invitationNo") String invitationNo, @Param("teamId") Long teamId,
                             @Param("inviterMemberId") Long inviterMemberId,
                             @Param("inviteeMemberId") Long inviteeMemberId,
                             @Param("inviteePlayerId") Long inviteePlayerId,
                             @Param("invitationStatus") Integer invitationStatus,
                             @Param("expireTime") Date expireTime, @Param("remark") String remark);

    int insertRegistration(@Param("registrationNo") String registrationNo, @Param("seasonId") Long seasonId,
                           @Param("teamId") Long teamId, @Param("captainMemberId") Long captainMemberId,
                           @Param("captainPlayerId") Long captainPlayerId, @Param("contactName") String contactName,
                           @Param("contactMobile") String contactMobile,
                           @Param("registrationStatus") Integer registrationStatus,
                           @Param("lineupCount") Integer lineupCount, @Param("remark") String remark,
                           @Param("submittedTime") Date submittedTime);

    int insertRegistrationPlayer(@Param("registrationId") Long registrationId, @Param("seasonId") Long seasonId,
                                 @Param("teamId") Long teamId, @Param("memberId") Long memberId,
                                 @Param("playerId") Long playerId, @Param("jerseyNo") String jerseyNo,
                                 @Param("rosterRole") String rosterRole, @Param("isStarting") Integer isStarting,
                                 @Param("isCaptain") Integer isCaptain,
                                 @Param("validationStatus") Integer validationStatus);

    int insertTournament(@Param("tournamentNo") String tournamentNo, @Param("tournamentName") String tournamentName,
                         @Param("shortName") String shortName, @Param("englishName") String englishName,
                         @Param("gameTerminal") String gameTerminal, @Param("formatCode") String formatCode,
                         @Param("tournamentStatus") Integer tournamentStatus);

    int insertSeason(@Param("seasonNo") String seasonNo, @Param("tournamentId") Long tournamentId,
                     @Param("seasonName") String seasonName, @Param("seasonYear") Integer seasonYear,
                     @Param("registrationStartTime") Date registrationStartTime,
                     @Param("registrationEndTime") Date registrationEndTime,
                     @Param("seasonStartTime") Date seasonStartTime, @Param("seasonEndTime") Date seasonEndTime,
                     @Param("maxTeamCount") Integer maxTeamCount, @Param("minRosterCount") Integer minRosterCount,
                     @Param("maxRosterCount") Integer maxRosterCount, @Param("seasonStatus") Integer seasonStatus,
                     @Param("publicStatus") Integer publicStatus);

    int insertStandingRule(@Param("seasonId") Long seasonId, @Param("ruleContent") String ruleContent);

    int insertStage(@Param("seasonId") Long seasonId, @Param("stageNo") String stageNo,
                    @Param("stageName") String stageName, @Param("stageType") String stageType,
                    @Param("formatType") String formatType, @Param("startTime") Date startTime,
                    @Param("endTime") Date endTime, @Param("sequenceNo") Integer sequenceNo,
                    @Param("stageStatus") Integer stageStatus);

    int insertRound(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId, @Param("roundNo") String roundNo,
                    @Param("roundName") String roundName, @Param("sequenceNo") Integer sequenceNo,
                    @Param("roundStatus") Integer roundStatus);

    int insertGroup(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId,
                    @Param("groupCode") String groupCode, @Param("groupName") String groupName,
                    @Param("groupStatus") Integer groupStatus);

    int insertGroupTeam(@Param("groupId") Long groupId, @Param("seasonId") Long seasonId,
                        @Param("stageId") Long stageId, @Param("teamId") Long teamId,
                        @Param("seedNo") Integer seedNo);

    int insertMatch(@Param("matchNo") String matchNo, @Param("seasonId") Long seasonId,
                    @Param("stageId") Long stageId, @Param("roundId") Long roundId,
                    @Param("groupId") Long groupId, @Param("homeTeamId") Long homeTeamId,
                    @Param("awayTeamId") Long awayTeamId, @Param("scheduledStartTime") Date scheduledStartTime,
                    @Param("matchStatus") Integer matchStatus, @Param("homeScore") Integer homeScore,
                    @Param("awayScore") Integer awayScore, @Param("dataStatus") Integer dataStatus,
                    @Param("sortNo") Integer sortNo);

    int insertMatchTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId,
                        @Param("sideType") String sideType);

    int insertAnnouncement(@Param("announcementNo") String announcementNo, @Param("title") String title,
                           @Param("summary") String summary, @Param("contentText") String contentText,
                           @Param("announcementStatus") Integer announcementStatus, @Param("isTop") Integer isTop,
                           @Param("publishTime") Date publishTime);

    int insertHomeBanner(@Param("bannerName") String bannerName, @Param("title") String title,
                         @Param("subtitle") String subtitle, @Param("imageFileId") Long imageFileId,
                         @Param("targetType") String targetType, @Param("targetValue") String targetValue,
                         @Param("sortNo") Integer sortNo, @Param("bannerStatus") Integer bannerStatus,
                         @Param("startTime") Date startTime);

    int updatePlayerNo(@Param("playerId") Long playerId, @Param("playerNo") String playerNo);

    int updatePlayerCurrentTeam(@Param("playerId") Long playerId, @Param("teamId") Long teamId);

    int updateTeamAuditStatus(@Param("teamId") Long teamId, @Param("status") int status);

    int updateTeamMemberCount(@Param("teamId") Long teamId, @Param("memberCount") Integer memberCount);

    int updateRegistrationStatus(@Param("registrationId") Long registrationId, @Param("status") int status);

    int updateSeasonCurrentStage(@Param("seasonId") Long seasonId, @Param("stageId") Long stageId);

    int updateSeasonStatus(@Param("seasonId") Long seasonId, @Param("status") int status);

    int updateTournamentStatus(@Param("tournamentId") Long tournamentId, @Param("status") int status);

    int updateMatchNo(@Param("matchId") Long matchId, @Param("matchNo") String matchNo);

    int deleteBannersByPrefix(@Param("prefix") String prefix);

    int deleteAnnouncementsByPrefix(@Param("prefix") String prefix);

    int deletePlayerRanksByDemoSeason(@Param("seasonId") Long seasonId);

    int deleteTeamRanksByDemoSeason(@Param("seasonId") Long seasonId);

    int deletePlayerSeasonStatsBySeasonId(@Param("seasonId") Long seasonId);

    int deleteTeamSeasonStatsBySeasonId(@Param("seasonId") Long seasonId);

    int deleteStandingsBySeasonId(@Param("seasonId") Long seasonId);

    int deletePlayerMatchStatsByMatchIds(@Param("matchIds") List<Long> matchIds);

    int deleteTeamMatchStatsByMatchIds(@Param("matchIds") List<Long> matchIds);

    int deletePeriodScoresByMatchIds(@Param("matchIds") List<Long> matchIds);

    int deleteLineupsByMatchIds(@Param("matchIds") List<Long> matchIds);

    int deleteMatchTeamsByMatchIds(@Param("matchIds") List<Long> matchIds);

    int deleteMatchStatusLogsByMatchIds(@Param("matchIds") List<Long> matchIds);

    int deleteMatchesByPrefix(@Param("prefix") String prefix);

    int deleteGroupTeamsByGroupId(@Param("groupId") Long groupId);

    int deleteGroupsByPrefix(@Param("prefix") String prefix);

    int deleteRoundsByPrefix(@Param("prefix") String prefix);

    int deleteStagesByPrefix(@Param("prefix") String prefix);

    int deleteTournamentRulesBySeasonId(@Param("seasonId") Long seasonId, @Param("marker") String marker);

    int deleteRegistrationAuditsByRegistrationIds(@Param("registrationIds") List<Long> registrationIds);

    int deleteRegistrationPlayersByRegistrationIds(@Param("registrationIds") List<Long> registrationIds);

    int deleteRegistrationsByPrefix(@Param("prefix") String prefix);

    int deleteTeamSeasonProfilesBySeasonId(@Param("seasonId") Long seasonId, @Param("prefix") String prefix);

    int deleteTeamMembersByTeamIds(@Param("teamIds") List<Long> teamIds);

    int deleteTeamInvitationsByTeamIds(@Param("teamIds") List<Long> teamIds);

    int deleteTeamAuditsByTeamIds(@Param("teamIds") List<Long> teamIds);

    int deleteTeamMembersByPrefix(@Param("prefix") String prefix);

    int deleteTeamInvitationsByPrefix(@Param("prefix") String prefix);

    int deleteTeamsByPrefix(@Param("prefix") String prefix);

    Long selectIdByInvitationNo(@Param("invitationNo") String invitationNo);

    Long selectIdByMembershipNo(@Param("membershipNo") String membershipNo);

    int deleteCaptainQualificationsByPrefix(@Param("prefix") String prefix, @Param("marker") String marker);

    int deleteCertAuditsByApplicationIds(@Param("applicationIds") List<Long> applicationIds);

    int deleteCertMaterialsByApplicationIds(@Param("applicationIds") List<Long> applicationIds);

    int deleteCertApplicationsByPrefix(@Param("prefix") String prefix);

    int deletePlayersByPrefix(@Param("prefix") String prefix);

    int deleteMemberProfilesByAccountIds(@Param("accountIds") List<Long> accountIds);

    int deletePrivacyByMemberIds(@Param("memberIds") List<Long> memberIds);

    int deleteNotificationByMemberIds(@Param("memberIds") List<Long> memberIds);

    int deleteAccountsByPrefix(@Param("prefix") String prefix);

    int deleteSeasonsByPrefix(@Param("prefix") String prefix);

    int deleteTournamentsByPrefix(@Param("prefix") String prefix);

    int deleteHeadAvatarFilesByPrefix(@Param("prefix") String prefix);

    int deletePlayerAvatarFilesByPrefix(@Param("prefix") String prefix);

    int updateTeamLogoFileId(@Param("teamId") Long teamId, @Param("logoFileId") Long logoFileId);

    int updateFileAssetAccessMeta(@Param("fileId") Long fileId, @Param("accessUrl") String accessUrl,
                                  @Param("usageType") String usageType, @Param("bucketName") String bucketName);

    int deletePlaceholderFilesByRemark(@Param("remark") String remark);
}
