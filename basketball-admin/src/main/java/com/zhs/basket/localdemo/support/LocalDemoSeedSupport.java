package com.zhs.basket.localdemo.support;


import com.zhs.basket.constant.BasketMatchConstants;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.enums.*;
import com.zhs.basket.localdemo.config.BasketLocalDemoDataProperties;
import com.zhs.basket.localdemo.constant.BasketLocalDemoConstants;
import com.zhs.basket.localdemo.dto.*;
import com.zhs.basket.localdemo.mapper.BasketLocalDemoDataMapper;
import com.zhs.basket.localdemo.resp.BasketLocalDemoAvatarSyncResp;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.req.*;
import com.zhs.basket.service.*;
import com.zhs.common.exception.ServiceException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
@Profile("local")
@ConditionalOnProperty(prefix = "cnpa.local-demo", name = "enabled", havingValue = "true")
public class LocalDemoSeedSupport {


    private static final String PLACEHOLDER_OBJECT_KEY = "LOCAL-DEMO-PLACEHOLDER";

    private static final Pattern DEMO_ACCOUNT_INDEX = Pattern.compile("LOCAL-DEMO-ACCOUNT-(\\d+)$");

    private static final String STANDING_RULE = """
            
            WIN_POINTS=2
            
            LOSS_POINTS=1
            
            TIE_BREAK=STANDING_POINTS,POINT_DIFF,POINTS_FOR,WINS,TEAM_ID
            
            """;


    private final Clock clock;

    private final BasketLocalDemoDataProperties properties;

    private final BasketLocalDemoDataMapper demoMapper;

    private final BasketPlayerProfileMapper profileMapper;

    private final PasswordEncoder passwordEncoder;

    private final IBasketPlayerCertService playerCertService;

    private final IBasketCaptainQualificationService captainQualificationService;

    private final IBasketTeamAuditService teamAuditService;

    private final IBasketRegistrationAuditService registrationAuditService;

    private final IBasketMatchLineupService matchLineupService;

    private final IBasketMatchScheduleService matchScheduleService;

    private final IBasketMatchScoreService matchScoreService;

    private final IBasketMatchStatService matchStatService;

    private final IBasketDataSettlementService dataSettlementService;


    public LocalDemoSeedSupport(Clock clock, BasketLocalDemoDataProperties properties,

                                BasketLocalDemoDataMapper demoMapper, BasketPlayerProfileMapper profileMapper,

                                PasswordEncoder passwordEncoder, IBasketPlayerCertService playerCertService,

                                IBasketCaptainQualificationService captainQualificationService,

                                IBasketTeamAuditService teamAuditService,

                                IBasketRegistrationAuditService registrationAuditService,

                                IBasketMatchLineupService matchLineupService,

                                IBasketMatchScheduleService matchScheduleService,

                                IBasketMatchScoreService matchScoreService,

                                IBasketMatchStatService matchStatService,

                                IBasketDataSettlementService dataSettlementService) {

        this.clock = clock;

        this.properties = properties;

        this.demoMapper = demoMapper;

        this.profileMapper = profileMapper;

        this.passwordEncoder = passwordEncoder;

        this.playerCertService = playerCertService;

        this.captainQualificationService = captainQualificationService;

        this.teamAuditService = teamAuditService;

        this.registrationAuditService = registrationAuditService;

        this.matchLineupService = matchLineupService;

        this.matchScheduleService = matchScheduleService;

        this.matchScoreService = matchScoreService;

        this.matchStatService = matchStatService;

        this.dataSettlementService = dataSettlementService;

    }


    public void validatePasswordConfigured() {

        if (properties.getDefaultPassword() == null || properties.getDefaultPassword().isBlank()) {

            throw new ServiceException("未配置本地演示账号密码");

        }

    }


    public BasketLocalDemoSeedContextDto executeFullSeed() {

        validatePasswordConfigured();

        BasketLocalDemoSeedContextDto ctx = new BasketLocalDemoSeedContextDto();

        Date now = Date.from(clock.instant());


        ensurePlaceholderFile(ctx);

        ensurePlayerAvatarFiles(ctx);

        ensureTeamLogoFiles(ctx);

        createAccounts(ctx);

        createAndApprovePlayerCertifications(ctx, now);

        createTournamentAndSeason(ctx, now);

        grantCaptainQualifications(ctx, now);

        createAndApproveTeams(ctx, now);

        assignTeamLogos(ctx);

        createTeamMembers(ctx, now);

        createAndApproveRegistrations(ctx, now);

        createStageRoundsAndGroup(ctx, now);

        createMatches(ctx);

        createMatchLineups(ctx);

        createConfirmedMatchData(ctx);

        rebuildSeasonStatistics(ctx);

        createAnnouncements(ctx, now);

        createHomeRecommendations(ctx, now);


        return ctx;

    }


    /**
     * 为已有演示账号批量补写球员头像，并补写球队 Logo，无需 reset + seed。
     */

    public BasketLocalDemoAvatarSyncResp syncDemoAvatars() {

        validatePasswordConfigured();

        long started = System.currentTimeMillis();

        String prefix = BasketLocalDemoConstants.BIZ_PREFIX;

        BasketLocalDemoAvatarSyncResp resp = new BasketLocalDemoAvatarSyncResp();

        resp.setMarker(properties.getMarker());

        resp.setStartedTime(started);

        resp.setMissingAvatarBefore(demoMapper.countDemoMembersMissingAvatar(prefix));

        List<DemoMemberAccountRow> accounts = demoMapper.selectDemoMemberAccounts(prefix);

        resp.setDemoAccountCount(accounts.size());

        if (accounts.isEmpty()) {

            resp.setSuccess(true);

            resp.setMessage("未找到演示账号，无需同步头像");

            resp.setFinishedTime(System.currentTimeMillis());

            resp.setDurationMillis(resp.getFinishedTime() - started);

            return resp;

        }


        BasketLocalDemoSeedContextDto ctx = new BasketLocalDemoSeedContextDto();

        ensurePlayerAvatarFiles(ctx);

        ensureTeamLogoFiles(ctx);

        resp.setPlayerAvatarFileCount(ctx.getPlayerAvatarFileIds().size());

        resp.setTeamLogoFileCount(ctx.getTeamLogoFileIds().size());


        int updated = 0;

        int skipped = 0;

        for (DemoMemberAccountRow account : accounts) {

            int playerIndex = parseAccountIndex(account.getAccountNo());

            if (playerIndex <= 0) {

                resp.getWarnings().add("跳过无效账号编号：" + account.getAccountNo());

                skipped++;

                continue;

            }

            Long avatarFileId = resolvePlayerAvatarFileId(ctx, playerIndex);

            if (avatarFileId == null) {

                resp.getWarnings().add("未找到头像文件映射：account=" + account.getAccountNo()

                        + ", playerIndex=" + playerIndex);

                skipped++;

                continue;

            }

            int rows = demoMapper.updateMemberProfileAvatar(account.getMemberId(), avatarFileId);

            if (rows > 0) {

                updated++;

            } else {

                skipped++;

            }

        }


        int teamLogoUpdated = assignTeamLogos(ctx);

        resp.setTeamLogoUpdatedCount(teamLogoUpdated);


        resp.setUpdatedCount(updated);

        resp.setSkippedCount(skipped);

        resp.setMissingAvatarAfter(demoMapper.countDemoMembersMissingAvatar(prefix));

        resp.setSuccess(true);

        resp.setMessage(updated > 0

                ? "已为 " + updated + " 个演示账号补写头像"

                : "演示账号头像已是最新，无需更新");

        resp.setFinishedTime(System.currentTimeMillis());

        resp.setDurationMillis(resp.getFinishedTime() - started);

        return resp;

    }


    private int parseAccountIndex(String accountNo) {

        Matcher matcher = DEMO_ACCOUNT_INDEX.matcher(accountNo);

        if (!matcher.matches()) {

            return -1;

        }

        return Integer.parseInt(matcher.group(1));

    }


    /**
     * 步骤1：创建占位文件，供认证材料与首页横幅引用
     */

    private void ensurePlaceholderFile(BasketLocalDemoSeedContextDto ctx) {

        String marker = properties.getMarker();

        Long fileId = demoMapper.selectPlaceholderFileIdByRemark(marker);

        if (fileId == null) {

            Long existingId = demoMapper.selectPlaceholderFileIdByRemarkAny(marker);

            if (existingId != null) {

                // reset 后占位文件可能被软删，唯一键仍占用 object_key，需恢复而非重复插入

                demoMapper.restorePlaceholderFileByRemark(marker);

                fileId = existingId;

            } else {

                demoMapper.insertPlaceholderFileAsset(PLACEHOLDER_OBJECT_KEY, marker);

                fileId = demoMapper.selectPlaceholderFileIdByRemark(marker);

            }

        }

        ctx.setPlaceholderFileId(fileId);

    }


    /**
     * 步骤1b：注册 player 目录默认球员头像，供演示会员 avatar_file_id 引用
     */

    private void ensurePlayerAvatarFiles(BasketLocalDemoSeedContextDto ctx) {

        String marker = properties.getMarker();

        for (int avatarNo = 1; avatarNo <= BasketLocalDemoConstants.PLAYER_AVATAR_COUNT; avatarNo++) {

            String objectKey = BasketLocalDemoConstants.playerAvatarObjectKey(avatarNo);

            Long fileId = demoMapper.selectFileIdByObjectKey(objectKey);

            if (fileId == null) {

                Long existingId = demoMapper.selectFileIdByObjectKeyAny(objectKey);

                if (existingId != null) {

                    demoMapper.restoreFileByObjectKey(objectKey);

                    fileId = existingId;

                } else {

                    String fileName = avatarNo + ".png";

                    demoMapper.insertDemoImageFileAsset(objectKey, fileName,

                            BasketLocalDemoConstants.playerAvatarAccessUrl(avatarNo),

                            "AVATAR", "player", marker);

                    fileId = demoMapper.selectFileIdByObjectKey(objectKey);

                }

            }

            demoMapper.updateFileAssetAccessMeta(fileId,

                    BasketLocalDemoConstants.playerAvatarAccessUrl(avatarNo),

                    "AVATAR", "player");

            ctx.getPlayerAvatarFileIds().put(avatarNo, fileId);

        }

    }


    /**
     * 步骤1c：注册 head 目录球队 Logo，供演示球队 logo_file_id 引用
     */

    private void ensureTeamLogoFiles(BasketLocalDemoSeedContextDto ctx) {

        String marker = properties.getMarker();

        for (int logoNo = 1; logoNo <= BasketLocalDemoConstants.TEAM_LOGO_COUNT; logoNo++) {

            String objectKey = BasketLocalDemoConstants.teamLogoObjectKey(logoNo);

            Long fileId = demoMapper.selectFileIdByObjectKey(objectKey);

            if (fileId == null) {

                Long existingId = demoMapper.selectFileIdByObjectKeyAny(objectKey);

                if (existingId != null) {

                    demoMapper.restoreFileByObjectKey(objectKey);

                    fileId = existingId;

                } else {

                    String fileName = logoNo + ".png";

                    demoMapper.insertDemoImageFileAsset(objectKey, fileName,

                            BasketLocalDemoConstants.teamLogoAccessUrl(logoNo),

                            "TEAM_LOGO", "head", marker);

                    fileId = demoMapper.selectFileIdByObjectKey(objectKey);

                }

            }

            demoMapper.updateFileAssetAccessMeta(fileId,

                    BasketLocalDemoConstants.teamLogoAccessUrl(logoNo),

                    "TEAM_LOGO", "head");

            ctx.getTeamLogoFileIds().put(logoNo, fileId);

        }

    }


    private Long resolvePlayerAvatarFileId(BasketLocalDemoSeedContextDto ctx, int playerIndex) {

        int avatarNo = BasketLocalDemoConstants.resolvePlayerAvatarNo(playerIndex);

        return ctx.getPlayerAvatarFileIds().get(avatarNo);

    }


    private Long resolveTeamLogoFileId(BasketLocalDemoSeedContextDto ctx, int teamIndex) {

        int logoNo = BasketLocalDemoConstants.resolveTeamLogoNo(teamIndex);

        return ctx.getTeamLogoFileIds().get(logoNo);

    }


    /**
     * 按球队定义顺序写入 logo_file_id（head 资源）。
     */

    private int assignTeamLogos(BasketLocalDemoSeedContextDto ctx) {

        int updated = 0;

        int teamIndex = 0;

        for (DemoTeamDto team : LocalDemoSeedDefinitions.teams()) {

            teamIndex++;

            Long teamId = ctx.getTeamIds().get(team.getShortName());

            if (teamId == null) {

                teamId = demoMapper.selectIdByTeamNo(team.getTeamNo());

            }

            if (teamId == null) {

                continue;

            }

            Long logoFileId = resolveTeamLogoFileId(ctx, teamIndex);

            if (logoFileId == null) {

                continue;

            }

            int rows = demoMapper.updateTeamLogoFileId(teamId, logoFileId);

            if (rows > 0) {

                updated++;

            }

            ctx.getTeamIds().put(team.getShortName(), teamId);

        }

        return updated;

    }


    /**
     * 步骤2：批量创建 128 个官网账号及会员资料
     */

    private void createAccounts(BasketLocalDemoSeedContextDto ctx) {

        String encoded = passwordEncoder.encode(properties.getDefaultPassword());

        for (DemoPlayerDto player : LocalDemoSeedDefinitions.players()) {

            int idx = player.getGlobalIndex();

            String accountNo = BasketLocalDemoConstants.accountNo(idx);

            Long memberId = demoMapper.selectIdByAccountNo(accountNo);

            if (memberId == null) {

                demoMapper.insertMemberAccount(accountNo, encoded,
                        BasketLocalDemoConstants.demoEmail(idx),

                        BasketLocalDemoConstants.demoUsername(idx),

                        BasketLocalDemoConstants.REGISTER_SOURCE, BasketLocalDemoConstants.GAME_TERMINAL);

                memberId = demoMapper.selectIdByAccountNo(accountNo);

                demoMapper.insertMemberProfile(memberId, player.getName(),

                        resolvePlayerAvatarFileId(ctx, idx));

                demoMapper.insertPrivacySetting(memberId);

                demoMapper.insertNotificationSetting(memberId);

            }

            ctx.getAccountIds().put(idx, memberId);

        }

    }


    /**
     * 步骤3：创建球员认证申请并走 Admin 审核通过链路
     */

    private void createAndApprovePlayerCertifications(BasketLocalDemoSeedContextDto ctx, Date now) {

        String marker = properties.getMarker();

        for (DemoPlayerDto player : LocalDemoSeedDefinitions.players()) {

            int idx = player.getGlobalIndex();

            Long memberId = ctx.getAccountIds().get(idx);

            String applicationNo = BasketLocalDemoConstants.certApplicationNo(idx);

            Long applicationId = demoMapper.selectIdByCertApplicationNo(applicationNo);

            if (applicationId == null) {

                demoMapper.insertCertApplication(applicationNo, memberId,

                        BasketLocalDemoConstants.GAME_TERMINAL,

                        "demo_game_" + idx, player.getName(), player.getPositionCode(),

                        PlayerCertStatusEnum.SUBMITTED.getCode(), now);

                applicationId = demoMapper.selectIdByCertApplicationNo(applicationNo);

                demoMapper.insertCertMaterial(applicationId, ctx.getPlaceholderFileId(),
                        "PLAYER_CERT_GAME_SCREENSHOT", marker);

            }

            ctx.getCertApplicationIds().put(idx, applicationId);


            playerCertService.startReview(applicationId);

            PlayerCertApproveReq approveReq = new PlayerCertApproveReq();

            approveReq.setAuditRemark(marker);

            approveReq.setPositionCode(player.getPositionCode());

            playerCertService.approve(applicationId, approveReq);


            BasketPlayerProfile profile = profileMapper.selectByMemberId(memberId);

            if (profile == null) {

                throw new ServiceException("球员档案生成失败：" + player.getName());

            }

            demoMapper.updatePlayerNo(profile.getId(), BasketLocalDemoConstants.playerNo(idx));

            ctx.getPlayerIds().put(idx, profile.getId());

        }

    }


    /**
     * 步骤4：为 16 支球队的队长授予有效队长授权（需先创建赛事赛季）
     */

    private void grantCaptainQualifications(BasketLocalDemoSeedContextDto ctx, Date now) {
        if (ctx.getSeasonId() == null) {
            throw new ServiceException("演示赛季未创建，无法授予队长授权");
        }
        String marker = properties.getMarker();
        for (DemoTeamDto team : LocalDemoSeedDefinitions.teams()) {
            String key = LocalDemoSeedDefinitions.teamKeyByNo().get(team.getTeamNo());
            DemoPlayerDto captain = findCaptain(key);
            Long playerId = ctx.getPlayerIds().get(captain.getGlobalIndex());

            CaptainQualificationGrantReq req = new CaptainQualificationGrantReq();
            req.setSeasonId(ctx.getSeasonId());
            req.setPlayerId(playerId);
            req.setPaymentConfirmed(1);
            req.setPaymentReference(marker);
            req.setGrantRemark(marker);
            captainQualificationService.grant(req);
        }
    }


    /**
     * 步骤5：创建球队并走 Admin 审核通过
     */

    private void createAndApproveTeams(BasketLocalDemoSeedContextDto ctx, Date now) {

        String marker = properties.getMarker();

        for (DemoTeamDto team : LocalDemoSeedDefinitions.teams()) {

            String key = LocalDemoSeedDefinitions.teamKeyByNo().get(team.getTeamNo());

            DemoPlayerDto captain = findCaptain(key);

            Long captainMemberId = ctx.getAccountIds().get(captain.getGlobalIndex());

            Long captainPlayerId = ctx.getPlayerIds().get(captain.getGlobalIndex());


            Long teamId = demoMapper.selectIdByTeamNo(team.getTeamNo());

            if (teamId == null) {

                demoMapper.insertTeam(team.getTeamNo(), team.getTeamName(), team.getShortName(), team.getEnglishName(),

                        team.getDescription(), BasketLocalDemoConstants.GAME_TERMINAL, captainMemberId, captainPlayerId,

                        20, 1, TeamAuditStatusEnum.PENDING.getCode());

                teamId = demoMapper.selectIdByTeamNo(team.getTeamNo());

                demoMapper.insertTeamMember("LOCAL-DEMO-MEM-" + team.getShortName() + "-CAP",

                        teamId, captainMemberId, captainPlayerId, TeamMemberRoleEnum.CAPTAIN.getCode(),

                        captain.getJerseyNo(), "CREATE", marker, now);

            }

            ctx.getTeamIds().put(key, teamId);


            teamAuditService.startReview(teamId);

            TeamAuditApproveReq approveReq = new TeamAuditApproveReq();

            approveReq.setAuditRemark(marker);

            teamAuditService.approve(teamId, approveReq);

            demoMapper.updatePlayerCurrentTeam(captainPlayerId, teamId);

        }

    }


    /**
     * 步骤6：邀请并入队其余 7 名球员，形成每队 8 人
     */

    private void createTeamMembers(BasketLocalDemoSeedContextDto ctx, Date now) {

        String marker = properties.getMarker();

        Calendar expire = Calendar.getInstance();

        expire.setTime(now);

        expire.add(Calendar.DAY_OF_MONTH, 7);

        Date expireTime = expire.getTime();


        for (DemoTeamDto team : LocalDemoSeedDefinitions.teams()) {

            String key = LocalDemoSeedDefinitions.teamKeyByNo().get(team.getTeamNo());

            Long teamId = ctx.getTeamIds().get(key);

            DemoPlayerDto captain = findCaptain(key);

            Long captainMemberId = ctx.getAccountIds().get(captain.getGlobalIndex());


            List<DemoPlayerDto> members = LocalDemoSeedDefinitions.players().stream()

                    .filter(p -> key.equals(p.getTeamKey()) && !p.isCaptain()).collect(Collectors.toList());

            for (DemoPlayerDto member : members) {

                Long memberId = ctx.getAccountIds().get(member.getGlobalIndex());

                Long playerId = ctx.getPlayerIds().get(member.getGlobalIndex());

                String membershipNo = "LOCAL-DEMO-MEM-" + team.getShortName() + "-" + member.getJerseyNo();


                String invitationNo = "LOCAL-DEMO-INV-" + team.getShortName() + "-" + member.getJerseyNo();

                if (demoMapper.selectIdByInvitationNo(invitationNo) == null) {

                    // 演示数据跳过用户端点击，但保留邀请与成员业务记录

                    demoMapper.insertTeamInvitation(invitationNo,

                            teamId, captainMemberId, memberId, playerId, 1, expireTime, marker);

                }

                if (demoMapper.selectIdByMembershipNo(membershipNo) == null) {

                    demoMapper.insertTeamMember(membershipNo, teamId, memberId, playerId, TeamMemberRoleEnum.MEMBER.getCode(),

                            member.getJerseyNo(), "INVITE", marker, now);

                }

                demoMapper.updatePlayerCurrentTeam(playerId, teamId);

            }

            demoMapper.updateTeamMemberCount(teamId, 8);

        }

    }


    /**
     * 步骤7：创建演示赛事、赛季与积分榜规则
     */

    private void createTournamentAndSeason(BasketLocalDemoSeedContextDto ctx, Date now) {

        LocalDate today = LocalDate.now(clock);

        Date regStart = Date.from(today.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Date regEnd = Date.from(today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Date seasonStart = Date.from(today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Date seasonEnd = Date.from(today.plusDays(60).atStartOfDay(ZoneId.systemDefault()).toInstant());


        Long tournamentId = demoMapper.selectIdByTournamentNo(BasketLocalDemoConstants.TOURNAMENT_NO);

        if (tournamentId == null) {

            demoMapper.insertTournament(BasketLocalDemoConstants.TOURNAMENT_NO, "CNPA 本地演示联赛", "CNPA DEMO",

                    "CNPA LOCAL DEMO LEAGUE", BasketLocalDemoConstants.GAME_TERMINAL, "5V5",

                    TournamentStatusEnum.ACTIVE.getCode());

            tournamentId = demoMapper.selectIdByTournamentNo(BasketLocalDemoConstants.TOURNAMENT_NO);

        }

        ctx.setTournamentId(tournamentId);


        Long seasonId = demoMapper.selectIdBySeasonNo(BasketLocalDemoConstants.SEASON_NO);

        if (seasonId == null) {

            demoMapper.insertSeason(BasketLocalDemoConstants.SEASON_NO, tournamentId, "2026 夏季演示赛",

                    today.getYear(), regStart, regEnd, seasonStart, seasonEnd,

                    BasketLocalDemoConstants.TEAM_COUNT, 5, BasketLocalDemoConstants.PLAYERS_PER_TEAM,

                    SeasonStatusEnum.IN_PROGRESS.getCode(), 1);

            seasonId = demoMapper.selectIdBySeasonNo(BasketLocalDemoConstants.SEASON_NO);

            demoMapper.insertStandingRule(seasonId, STANDING_RULE);

        }

        ctx.setSeasonId(seasonId);

    }


    /**
     * 步骤8：16 支球队提交报名并审核通过
     */

    private void createAndApproveRegistrations(BasketLocalDemoSeedContextDto ctx, Date now) {

        String marker = properties.getMarker();

        for (DemoTeamDto team : LocalDemoSeedDefinitions.teams()) {

            String key = LocalDemoSeedDefinitions.teamKeyByNo().get(team.getTeamNo());

            Long teamId = ctx.getTeamIds().get(key);

            DemoPlayerDto captain = findCaptain(key);

            String registrationNo = "LOCAL-DEMO-REG-" + team.getShortName();

            Long registrationId = demoMapper.selectIdByRegistrationNo(registrationNo);

            if (registrationId == null) {

                demoMapper.insertRegistration(registrationNo, ctx.getSeasonId(), teamId,

                        ctx.getAccountIds().get(captain.getGlobalIndex()), ctx.getPlayerIds().get(captain.getGlobalIndex()),

                        captain.getName(), BasketLocalDemoConstants.demoMobile(captain.getGlobalIndex()),

                        RegistrationStatusEnum.REVIEWING.getCode(), 8, marker, now);

                registrationId = demoMapper.selectIdByRegistrationNo(registrationNo);


                List<DemoPlayerDto> roster = LocalDemoSeedDefinitions.players().stream()

                        .filter(p -> key.equals(p.getTeamKey())).collect(Collectors.toList());

                for (int i = 0; i < roster.size(); i++) {

                    DemoPlayerDto p = roster.get(i);

                    demoMapper.insertRegistrationPlayer(registrationId, ctx.getSeasonId(), teamId,

                            ctx.getAccountIds().get(p.getGlobalIndex()), ctx.getPlayerIds().get(p.getGlobalIndex()),

                            p.getJerseyNo(), p.isCaptain() ? RosterRoleEnum.CAPTAIN.getCode() : RosterRoleEnum.PLAYER.getCode(),

                            i < 5 ? 1 : 0, p.isCaptain() ? 1 : 0,

                            BasketMatchConstants.REGISTRATION_PLAYER_PASS);

                }

            }

            ctx.getRegistrationIds().put(key, registrationId);

            RegistrationAuditApproveReq approveReq = new RegistrationAuditApproveReq();

            approveReq.setAuditRemark(marker);

            registrationAuditService.approve(registrationId, approveReq);

        }

    }


    /**
     * 步骤9：创建阶段、轮次、四个小组及球队分组关系
     */

    private void createStageRoundsAndGroup(BasketLocalDemoSeedContextDto ctx, Date now) {

        LocalDate today = LocalDate.now(clock);

        Date stageStart = Date.from(today.minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Date stageEnd = Date.from(today.plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());


        Long stageId = demoMapper.selectIdByStageNo(BasketLocalDemoConstants.STAGE_NO);

        if (stageId == null) {

            demoMapper.insertStage(ctx.getSeasonId(), BasketLocalDemoConstants.STAGE_NO, "小组循环赛",

                    "GROUP", "ROUND_ROBIN", stageStart, stageEnd, 1,

                    TournamentStageStatusEnum.ACTIVE.getCode());

            stageId = demoMapper.selectIdByStageNo(BasketLocalDemoConstants.STAGE_NO);

        }

        ctx.setStageId(stageId);

        demoMapper.updateSeasonCurrentStage(ctx.getSeasonId(), stageId);


        for (int i = 1; i <= BasketLocalDemoConstants.ROUND_COUNT; i++) {

            Long roundId = demoMapper.selectIdByRoundNo(BasketLocalDemoConstants.SEASON_NO, i);

            if (roundId == null) {

                String roundNo = "LOCAL-DEMO-ROUND-" + i;

                demoMapper.insertRound(ctx.getSeasonId(), stageId, roundNo, "第" + i + "轮", i,

                        TournamentRoundStatusEnum.SCHEDULED.getCode());

                roundId = demoMapper.selectIdByRoundNo(BasketLocalDemoConstants.SEASON_NO, i);

            }

            ctx.getRoundIds().put(i, roundId);

        }


        for (String groupKey : LocalDemoSeedDefinitions.GROUP_KEYS) {

            String groupCode = BasketLocalDemoConstants.groupCode(groupKey);

            Long groupId = demoMapper.selectIdByGroupCode(groupCode);

            if (groupId == null) {

                demoMapper.insertGroup(ctx.getSeasonId(), stageId, groupCode, groupKey + " 组",

                        groupKey.charAt(0) - 'A' + 1);

                groupId = demoMapper.selectIdByGroupCode(groupCode);

            }

            ctx.getGroupIds().put(groupKey, groupId);


            int seed = 1;

            for (DemoTeamDto team : LocalDemoSeedDefinitions.teams()) {

                if (!groupKey.equals(team.getGroupKey())) {

                    continue;

                }

                String key = team.getShortName();

                demoMapper.insertGroupTeam(groupId, ctx.getSeasonId(), stageId, ctx.getTeamIds().get(key), seed);

                seed++;

            }

        }

    }


    /**
     * 步骤10：创建 24 场比赛（16 场已确认 + 8 场未来赛程）
     */

    private void createMatches(BasketLocalDemoSeedContextDto ctx) {

        for (DemoMatchDto match : LocalDemoSeedDefinitions.matches()) {

            Long matchId = demoMapper.selectIdByMatchNo(match.getMatchNo());

            if (matchId != null) {

                ctx.getMatchIds().put(match.getMatchIndex(), matchId);

                continue;

            }

            Long homeTeamId = ctx.getTeamIds().get(match.getHomeTeamKey());

            Long awayTeamId = ctx.getTeamIds().get(match.getAwayTeamKey());

            LocalDate day = LocalDate.now(clock).plusDays(match.getDayOffset());

            Date scheduled = Date.from(day.atTime(19, 30).atZone(ZoneId.systemDefault()).toInstant());

            demoMapper.insertMatch(match.getMatchNo(), ctx.getSeasonId(), ctx.getStageId(),

                    ctx.getRoundIds().get(match.getRoundNo()), ctx.getGroupIds().get(match.getGroupKey()),

                    homeTeamId, awayTeamId, scheduled,

                    MatchStatusEnum.SCHEDULED.getCode(), 0, 0, MatchDataStatusEnum.DRAFT.getCode(),

                    match.getMatchIndex());

            matchId = demoMapper.selectIdByMatchNo(match.getMatchNo());

            demoMapper.insertMatchTeam(matchId, homeTeamId, "HOME");

            demoMapper.insertMatchTeam(matchId, awayTeamId, "AWAY");

            ctx.getMatchIds().put(match.getMatchIndex(), matchId);

        }

    }


    /**
     * 步骤11：为全部比赛生成出场名单
     */

    private void createMatchLineups(BasketLocalDemoSeedContextDto ctx) {

        for (DemoMatchDto match : LocalDemoSeedDefinitions.matches()) {

            Long matchId = ctx.getMatchIds().get(match.getMatchIndex());

            saveTeamLineup(matchId, ctx, match.getHomeTeamKey());

            saveTeamLineup(matchId, ctx, match.getAwayTeamKey());

        }

    }


    private void saveTeamLineup(Long matchId, BasketLocalDemoSeedContextDto ctx, String teamKey) {

        Long registrationId = ctx.getRegistrationIds().get(teamKey);

        List<DemoPlayerDto> roster = LocalDemoSeedDefinitions.players().stream()

                .filter(p -> teamKey.equals(p.getTeamKey())).collect(Collectors.toList());

        MatchLineupSaveReq req = new MatchLineupSaveReq();

        List<MatchLineupSaveReq.LineupPlayerItem> items = new ArrayList<>();

        for (int i = 0; i < roster.size(); i++) {

            DemoPlayerDto p = roster.get(i);

            Long regPlayerId = demoMapper.selectRegistrationPlayerId(registrationId,

                    ctx.getPlayerIds().get(p.getGlobalIndex()));

            MatchLineupSaveReq.LineupPlayerItem item = new MatchLineupSaveReq.LineupPlayerItem();

            item.setRegistrationPlayerId(regPlayerId);

            item.setJerseyNo(p.getJerseyNo());

            item.setIsStarting(i < 5 ? 1 : 0);

            item.setCaptainFlag(p.isCaptain() ? 1 : 0);

            item.setRosterRole(p.isCaptain() ? RosterRoleEnum.CAPTAIN.getCode() : RosterRoleEnum.PLAYER.getCode());

            item.setPositionCode(p.getPositionCode());

            item.setSortNo(i + 1);

            items.add(item);

        }

        req.setPlayers(items);

        matchLineupService.saveLineup(matchId, ctx.getTeamIds().get(teamKey), req);

    }


    /**
     * 步骤12：为 16 场已结束比赛录入比分、技术统计并确认结果
     */

    private void createConfirmedMatchData(BasketLocalDemoSeedContextDto ctx) {

        for (DemoMatchDto match : LocalDemoSeedDefinitions.matches()) {

            if (!match.isConfirmed()) {

                continue;

            }

            Long matchId = ctx.getMatchIds().get(match.getMatchIndex());


            matchScheduleService.start(matchId);


            MatchPeriodScoreSaveReq periodReq = new MatchPeriodScoreSaveReq();

            List<MatchPeriodScoreSaveReq.PeriodScoreItem> periods = new ArrayList<>();

            int[] homePeriods = LocalDemoSeedDefinitions.periodScores(match.getHomeScore(), stableSeed(match.getMatchNo(), 1));

            int[] awayPeriods = LocalDemoSeedDefinitions.periodScores(match.getAwayScore(), stableSeed(match.getMatchNo(), 2));

            for (int i = 0; i < 4; i++) {

                MatchPeriodScoreSaveReq.PeriodScoreItem item = new MatchPeriodScoreSaveReq.PeriodScoreItem();

                item.setPeriodNo(i + 1);

                item.setPeriodType(MatchPeriodTypeEnum.REGULAR.getCode());

                item.setHomeScore(homePeriods[i]);

                item.setAwayScore(awayPeriods[i]);

                periods.add(item);

            }

            periodReq.setPeriods(periods);

            matchScoreService.savePeriodScores(matchId, periodReq);

            matchScoreService.submitScore(matchId);


            saveMatchStats(matchId, ctx, match);

            matchStatService.submitPlayerStats(matchId);

            matchStatService.submitTeamStats(matchId);

            matchStatService.confirmResult(matchId);

        }

    }


    private void saveMatchStats(Long matchId, BasketLocalDemoSeedContextDto ctx, DemoMatchDto match) {

        MatchPlayerStatSaveReq playerReq = new MatchPlayerStatSaveReq();

        List<MatchPlayerStatSaveReq.PlayerStatItem> allPlayerStats = new ArrayList<>();

        allPlayerStats.addAll(buildSidePlayerStats(ctx, match.getHomeTeamKey(), match.getHomeScore(), match.getMatchNo()));

        allPlayerStats.addAll(buildSidePlayerStats(ctx, match.getAwayTeamKey(), match.getAwayScore(), match.getMatchNo()));

        playerReq.setStats(allPlayerStats);

        matchStatService.savePlayerStats(matchId, playerReq);


        MatchTeamStatSaveReq teamReq = new MatchTeamStatSaveReq();

        List<MatchTeamStatSaveReq.TeamStatItem> teamStats = new ArrayList<>();

        teamStats.addAll(buildSideTeamStats(ctx, match.getHomeTeamKey(), match.getHomeScore(), match.getMatchNo()));

        teamStats.addAll(buildSideTeamStats(ctx, match.getAwayTeamKey(), match.getAwayScore(), match.getMatchNo()));

        teamReq.setStats(teamStats);

        matchStatService.saveTeamStats(matchId, teamReq);

    }


    private List<MatchPlayerStatSaveReq.PlayerStatItem> buildSidePlayerStats(BasketLocalDemoSeedContextDto ctx,

                                                                             String teamKey, int teamScore, String matchNo) {

        Long teamId = ctx.getTeamIds().get(teamKey);

        List<DemoPlayerDto> roster = LocalDemoSeedDefinitions.players().stream()

                .filter(p -> teamKey.equals(p.getTeamKey())).collect(Collectors.toList());

        MatchPlayerStatSaveReq playerReq = LocalDemoStatGenerator.buildPlayerStats(null, matchNo, teamId, roster, teamScore, true);

        List<Long> playerIds = roster.stream().map(p -> ctx.getPlayerIds().get(p.getGlobalIndex())).collect(Collectors.toList());

        LocalDemoStatGenerator.bindPlayerIds(playerReq, playerIds);

        return playerReq.getStats();

    }


    private List<MatchTeamStatSaveReq.TeamStatItem> buildSideTeamStats(BasketLocalDemoSeedContextDto ctx,

                                                                       String teamKey, int teamScore, String matchNo) {

        List<MatchPlayerStatSaveReq.PlayerStatItem> playerStats = buildSidePlayerStats(ctx, teamKey, teamScore, matchNo);

        Long teamId = ctx.getTeamIds().get(teamKey);

        return LocalDemoStatGenerator.aggregateTeamStats(teamId, playerStats).getStats();

    }


    /**
     * 步骤13：调用真实数据结算服务生成积分榜、赛季统计与排行榜
     */

    private void rebuildSeasonStatistics(BasketLocalDemoSeedContextDto ctx) {

        dataSettlementService.rebuildSeason(ctx.getSeasonId());

    }


    /**
     * 步骤14：创建 3 条已发布公告
     */

    private void createAnnouncements(BasketLocalDemoSeedContextDto ctx, Date now) {

        LocalDate today = LocalDate.now(clock);

        insertAnnouncement(1, "CNPA 本地演示联赛正式开启",

                "十六支虚构战队已完成报名，四组小组循环赛正式打响。",

                "欢迎体验 CNPA 本地演示环境。本联赛全部球队与球员均为虚构数据，用于开发联调与业务演示。",

                today.minusDays(2), 1, ctx);

        insertAnnouncement(2, "2026 夏季演示赛第三轮赛程公布",

                "四个小组第三轮共八场焦点战即将开赛。",

                "第三轮赛程已更新：各小组末轮对决将决定出线形势，请关注赛事中心最新对阵。",

                today.minusDays(1), 0, ctx);

        insertAnnouncement(3, "球队阵容与赛事数据说明",

                "演示环境数据由初始化接口生成，可重复 seed 与 reset。",

                "如需清理演示数据，请使用 Admin 本地 reset 接口并传入 confirmMarker。",

                today, 0, ctx);

    }


    private void insertAnnouncement(int index, String title, String summary, String content,

                                    LocalDate publishDay, int top, BasketLocalDemoSeedContextDto ctx) {

        String no = BasketLocalDemoConstants.announcementNo(index);

        Long existing = demoMapper.selectIdByAnnouncementNo(no);

        if (existing != null) {

            ctx.getAnnouncementIds().add(existing);

            return;

        }

        Date publishTime = Date.from(publishDay.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant());

        demoMapper.insertAnnouncement(no, title, summary, content, 1, top, publishTime);

        ctx.getAnnouncementIds().add(demoMapper.selectIdByAnnouncementNo(no));

    }


    /**
     * 步骤15：创建 3 条首页运营推荐横幅
     */

    private void createHomeRecommendations(BasketLocalDemoSeedContextDto ctx, Date now) {

        LocalDate today = LocalDate.now(clock);

        Date start = Date.from(today.minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());

        insertBanner(1, "2026 夏季演示赛火热进行中", "十六支战队四组对决，小组循环赛正式开战。",

                "TOURNAMENT", String.valueOf(ctx.getSeasonId()), 10, start, ctx);

        insertBanner(2, "雷霆猎手 A 组领跑", "小组赛阶段保持领先，值得关注。",

                "TEAM", String.valueOf(ctx.getTeamIds().get(LocalDemoSeedDefinitions.TEAM_THH)), 20, start, ctx);

        Long ann2 = demoMapper.selectIdByAnnouncementNo(BasketLocalDemoConstants.announcementNo(2));

        insertBanner(3, "第三轮赛程已经公布", "四个小组末轮八场比赛即将打响。",

                "ANNOUNCEMENT", String.valueOf(ann2), 30, start, ctx);

    }


    private void insertBanner(int index, String title, String subtitle, String targetType, String targetValue,

                              int sortNo, Date startTime, BasketLocalDemoSeedContextDto ctx) {

        String name = BasketLocalDemoConstants.bannerName(index);

        Long existing = demoMapper.selectIdByBannerName(name);

        if (existing != null) {

            ctx.getBannerIds().add(existing);

            return;

        }

        demoMapper.insertHomeBanner(name, title, subtitle, ctx.getPlaceholderFileId(), targetType, targetValue, sortNo, 1, startTime);

        ctx.getBannerIds().add(demoMapper.selectIdByBannerName(name));

    }


    private DemoPlayerDto findCaptain(String teamKey) {

        return LocalDemoSeedDefinitions.players().stream()

                .filter(p -> teamKey.equals(p.getTeamKey()) && p.isCaptain())

                .findFirst()

                .orElseThrow(() -> new ServiceException("缺少队长定义：" + teamKey));

    }


    private long stableSeed(String matchNo, int side) {

        return 20260724L + matchNo.hashCode() * 31L + side;

    }

}

