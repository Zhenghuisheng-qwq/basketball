package com.zhs.support.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.response.v4.V4MatchSubmissionEligiblePlayerResp;
import com.zhs.response.v4.publicapi.V4PublicEventPlayerItemResp;
import com.zhs.response.v4.publicapi.V4PublicEventTeamDetailResp;
import com.zhs.response.v4.publicapi.V4PublicEventTeamItemResp;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesDetailResp;
import com.zhs.response.v4.publicapi.V4PublicScheduleItemResp;
import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;
import com.zhs.response.v4.publicapi.V4PublicTeamStatItemResp;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import com.zhs.support.GameTerminalSupport;
import com.zhs.support.MemberAvatarSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 官网「当前」展示读取最新合法资料，不回写 EventTeam / EventPlayer / 比赛统计快照。
 */
@Component
public class PublicCurrentRosterSupport {

    public static final String ACTIVE = "ACTIVE";

    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketPublicSupport publicSupport;

    public boolean isCurrentRosterPlayer(BasketEventPlayer player) {
        if (player == null || !Objects.equals(player.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            return false;
        }
        String status = player.getEventPlayerStatus();
        return !StringUtils.hasText(status) || ACTIVE.equalsIgnoreCase(status.trim());
    }

    /**
     * 尚未开赛的当前展示才用已生效球队资料。进行中、已结束、已弃赛保持赛事快照。
     */
    public static boolean useCurrentIdentity(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return switch (status.trim().toUpperCase()) {
            case "SCHEDULED", "PENDING", "READY" -> true;
            default -> false;
        };
    }

    /**
     * 系列赛已经开打或结束时，尚未开赛的子局也沿用系列赛快照，避免同一系列在不同页面各用一套队名和 Logo。
     */
    public static boolean useCurrentIdentity(String unitStatus, String parentSeriesStatus) {
        if (StringUtils.hasText(parentSeriesStatus) && !useCurrentIdentity(parentSeriesStatus)) {
            return false;
        }
        return useCurrentIdentity(unitStatus);
    }

    /**
     * 已审核并写入长期球队的名称、简称、Logo。空字段不返回，调用方因此不会用空值盖掉快照。
     * 未审核的资料变更还在申请表里，这里读不到。
     */
    public static CurrentTeamFace faceFrom(BasketTeam source, String logoUrl) {
        if (source == null || !Objects.equals(source.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            return null;
        }
        String name = text(source.getTeamName());
        String shortName = text(source.getShortName());
        Long logoFileId = source.getLogoFileId();
        if (name == null && shortName == null && logoFileId == null) {
            return null;
        }
        return new CurrentTeamFace(name, shortName, logoFileId, logoFileId == null ? null : logoUrl);
    }

    public Map<Long, CurrentTeamFace> loadCurrentFaces(Collection<BasketEventTeam> teams) {
        Map<Long, CurrentTeamFace> faces = new HashMap<>();
        if (CollectionUtils.isEmpty(teams)) {
            return faces;
        }
        Map<Long, Long> eventToSource = new HashMap<>();
        Set<Long> sourceIds = new HashSet<>();
        for (BasketEventTeam team : teams) {
            if (team == null || team.getId() == null || team.getSourceTeamId() == null) {
                continue;
            }
            eventToSource.put(team.getId(), team.getSourceTeamId());
            sourceIds.add(team.getSourceTeamId());
        }
        if (sourceIds.isEmpty()) {
            return faces;
        }
        List<BasketTeam> sources = teamDao.selectList(new LambdaQueryWrapper<BasketTeam>()
                .in(BasketTeam::getId, sourceIds)
                .eq(BasketTeam::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Map<Long, BasketTeam> sourceMap = new HashMap<>();
        Set<Long> logoIds = new HashSet<>();
        for (BasketTeam source : sources) {
            sourceMap.put(source.getId(), source);
            if (source.getLogoFileId() != null) {
                logoIds.add(source.getLogoFileId());
            }
        }
        Map<Long, String> logoUrls = publicSupport.loadAccessUrlMap(logoIds);
        for (Map.Entry<Long, Long> entry : eventToSource.entrySet()) {
            BasketTeam source = sourceMap.get(entry.getValue());
            String logoUrl = source == null || source.getLogoFileId() == null
                    ? null : logoUrls.get(source.getLogoFileId());
            CurrentTeamFace face = faceFrom(source, logoUrl);
            if (face != null) {
                faces.put(entry.getKey(), face);
            }
        }
        return faces;
    }

    public static void applyItem(V4PublicEventTeamItemResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setTeamName(face.teamName());
        }
        if (face.shortName() != null) {
            vo.setTeamShortName(face.shortName());
        }
        applyLogo(face, vo::setTeamLogoFileId, vo::setTeamLogoUrl);
    }

    public static void applyMatchHome(V4PublicMatchItemResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setHomeTeamName(face.teamName());
        }
        applyLogo(face, vo::setHomeTeamLogoFileId, vo::setHomeTeamLogoUrl);
    }

    public static void applyMatchAway(V4PublicMatchItemResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setAwayTeamName(face.teamName());
        }
        applyLogo(face, vo::setAwayTeamLogoFileId, vo::setAwayTeamLogoUrl);
    }

    public static void applyScheduleHome(V4PublicScheduleItemResp item, CurrentTeamFace face) {
        if (item == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            item.setHomeTeam(face.teamName());
        }
        if (face.shortName() != null) {
            item.setHomeTeamShortName(face.shortName());
            item.setHomeTeamCode(face.shortName());
        }
        applyLogo(face, item::setHomeTeamLogoFileId, item::setHomeTeamLogoUrl);
    }

    public static void applyScheduleAway(V4PublicScheduleItemResp item, CurrentTeamFace face) {
        if (item == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            item.setAwayTeam(face.teamName());
        }
        if (face.shortName() != null) {
            item.setAwayTeamShortName(face.shortName());
            item.setAwayTeamCode(face.shortName());
        }
        applyLogo(face, item::setAwayTeamLogoFileId, item::setAwayTeamLogoUrl);
    }

    public static void applySeriesHome(V4PublicMatchSeriesDetailResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setHomeTeamName(face.teamName());
        }
        if (face.shortName() != null) {
            vo.setHomeTeamShortName(face.shortName());
        }
        applyLogo(face, vo::setHomeTeamLogoFileId, vo::setHomeTeamLogoUrl);
    }

    public static void applySeriesAway(V4PublicMatchSeriesDetailResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setAwayTeamName(face.teamName());
        }
        if (face.shortName() != null) {
            vo.setAwayTeamShortName(face.shortName());
        }
        applyLogo(face, vo::setAwayTeamLogoFileId, vo::setAwayTeamLogoUrl);
    }

    public static void applyStanding(V4PublicStandingItemResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setTeamName(face.teamName());
        }
        applyLogo(face, vo::setTeamLogoFileId, vo::setTeamLogoUrl);
    }

    public static void applyTeamStat(V4PublicTeamStatItemResp vo, CurrentTeamFace face) {
        if (vo == null || face == null) {
            return;
        }
        if (face.teamName() != null) {
            vo.setTeamName(face.teamName());
        }
        applyLogo(face, vo::setTeamLogoFileId, vo::setTeamLogoUrl);
    }

    public void overlayTeamDetail(V4PublicEventTeamDetailResp resp, BasketEventTeam eventTeam) {
        if (resp == null || eventTeam == null) {
            return;
        }
        overlaySourceTeam(resp, eventTeam.getSourceTeamId());
        overlayPlayers(resp.getPlayers());
        if (!CollectionUtils.isEmpty(resp.getPlayers())) {
            for (V4PublicEventPlayerItemResp player : resp.getPlayers()) {
                if (player != null && Integer.valueOf(1).equals(player.getIsCaptain())
                        && StringUtils.hasText(player.getPlayerName())) {
                    resp.setCaptainName(player.getPlayerName());
                    break;
                }
            }
        }
    }

    private void overlaySourceTeam(V4PublicEventTeamDetailResp resp, Long sourceTeamId) {
        if (sourceTeamId == null) {
            return;
        }
        BasketTeam source = teamDao.selectById(sourceTeamId);
        if (source == null || !Objects.equals(source.getIsDelete(), BasketPublicConstants.NOT_DELETED)) {
            return;
        }
        String logoUrl = null;
        if (source.getLogoFileId() != null) {
            logoUrl = publicSupport.loadAccessUrlMap(Collections.singleton(source.getLogoFileId()))
                    .get(source.getLogoFileId());
        }
        applyItem(resp, faceFrom(source, logoUrl));
        if (StringUtils.hasText(source.getDescription())) {
            resp.setDescription(source.getDescription());
        }
    }

    private static void applyLogo(CurrentTeamFace face, Consumer<Long> logoId, Consumer<String> logoUrl) {
        if (face.logoFileId() == null) {
            return;
        }
        logoId.accept(face.logoFileId());
        logoUrl.accept(face.logoUrl());
    }

    private static String text(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record CurrentTeamFace(String teamName, String shortName, Long logoFileId, String logoUrl) {
    }

    /**
     * 提交页球员下拉与球队页「本赛季阵容」使用同一套当前昵称和游戏 ID。
     * 报名快照里的 CNPA 用户号不再当作队长认人的名字。
     */
    /** 与球队总览同一套：会员昵称优先，其次游戏昵称。昵称改了，这里下次读取就是新的。 */
    public Map<Long, String> loadCurrentNames(Collection<BasketEventPlayer> players) {
        Map<Long, String> result = new HashMap<>();
        if (CollectionUtils.isEmpty(players)) {
            return result;
        }
        Set<Long> sourceIds = new HashSet<>();
        for (BasketEventPlayer player : players) {
            if (player != null && player.getSourcePlayerId() != null) {
                sourceIds.add(player.getSourcePlayerId());
            }
        }
        Map<Long, BasketPlayerProfile> profiles = sourceIds.isEmpty() ? Map.of() : loadProfiles(sourceIds);
        Set<Long> memberIds = new HashSet<>();
        for (BasketPlayerProfile profile : profiles.values()) {
            if (profile.getMemberId() != null) {
                memberIds.add(profile.getMemberId());
            }
        }
        Map<Long, BasketMemberProfile> members = loadMembers(memberIds);
        for (BasketEventPlayer player : players) {
            if (player == null || player.getId() == null || player.getSourcePlayerId() == null) {
                continue;
            }
            BasketPlayerProfile profile = profiles.get(player.getSourcePlayerId());
            if (profile == null) {
                continue;
            }
            BasketMemberProfile member = profile.getMemberId() == null ? null : members.get(profile.getMemberId());
            String name = member != null && StringUtils.hasText(member.getNickname())
                    ? member.getNickname().trim()
                    : profile.getGameNickname();
            if (StringUtils.hasText(name)) {
                result.put(player.getId(), name.trim());
            }
        }
        return result;
    }

    public Map<Long, String> loadCurrentAvatarUrls(Collection<BasketEventPlayer> players) {
        Map<Long, String> result = new HashMap<>();
        if (CollectionUtils.isEmpty(players)) {
            return result;
        }
        Set<Long> sourceIds = new HashSet<>();
        Set<Long> fileIds = new HashSet<>();
        for (BasketEventPlayer player : players) {
            if (player == null) {
                continue;
            }
            if (player.getSourcePlayerId() != null) {
                sourceIds.add(player.getSourcePlayerId());
            }
            if (player.getAvatarFileId() != null) {
                fileIds.add(player.getAvatarFileId());
            }
        }
        Map<Long, BasketPlayerProfile> profiles = sourceIds.isEmpty() ? Map.of() : loadProfiles(sourceIds);
        Set<Long> memberIds = new HashSet<>();
        for (BasketPlayerProfile profile : profiles.values()) {
            if (profile.getMemberId() != null) {
                memberIds.add(profile.getMemberId());
            }
        }
        Map<Long, BasketMemberProfile> members = loadMembers(memberIds);
        for (BasketMemberProfile member : members.values()) {
            if (member.getAvatarFileId() != null) {
                fileIds.add(member.getAvatarFileId());
            }
        }
        Map<Long, String> urls = publicSupport.loadAccessUrlMap(fileIds);
        for (BasketEventPlayer player : players) {
            if (player == null || player.getId() == null) {
                continue;
            }
            Long fileId = null;
            BasketPlayerProfile profile = player.getSourcePlayerId() == null ? null : profiles.get(player.getSourcePlayerId());
            if (profile != null && profile.getMemberId() != null) {
                BasketMemberProfile member = members.get(profile.getMemberId());
                if (member != null) {
                    fileId = member.getAvatarFileId();
                }
            }
            if (fileId == null) {
                fileId = player.getAvatarFileId();
            }
            String url = fileId == null ? null : urls.get(fileId);
            if (StringUtils.hasText(url)) {
                result.put(player.getId(), url.trim());
            }
        }
        return result;
    }

    public void applyEligibleNames(List<BasketEventPlayer> players, List<V4MatchSubmissionEligiblePlayerResp> items) {
        if (CollectionUtils.isEmpty(players) || CollectionUtils.isEmpty(items)) {
            return;
        }
        Set<Long> playerIds = new HashSet<>();
        for (BasketEventPlayer player : players) {
            if (player != null && player.getSourcePlayerId() != null) {
                playerIds.add(player.getSourcePlayerId());
            }
        }
        if (playerIds.isEmpty()) {
            return;
        }
        Map<Long, BasketPlayerProfile> profileMap = loadProfiles(playerIds);
        Set<Long> memberIds = new HashSet<>();
        for (BasketPlayerProfile profile : profileMap.values()) {
            if (profile.getMemberId() != null) {
                memberIds.add(profile.getMemberId());
            }
        }
        Map<Long, BasketMemberProfile> memberMap = loadMembers(memberIds);
        Map<Long, List<BasketPlayerPlatformAccount>> accounts = loadAccounts(playerIds);
        Map<Long, BasketEventPlayer> byEventPlayerId = new HashMap<>();
        for (BasketEventPlayer player : players) {
            if (player != null && player.getId() != null) {
                byEventPlayerId.put(player.getId(), player);
            }
        }
        for (V4MatchSubmissionEligiblePlayerResp item : items) {
            if (item == null || item.getEventPlayerId() == null) {
                continue;
            }
            BasketEventPlayer eventPlayer = byEventPlayerId.get(item.getEventPlayerId());
            if (eventPlayer == null || eventPlayer.getSourcePlayerId() == null) {
                continue;
            }
            BasketPlayerProfile profile = profileMap.get(eventPlayer.getSourcePlayerId());
            if (profile == null) {
                continue;
            }
            BasketMemberProfile member = profile.getMemberId() == null ? null : memberMap.get(profile.getMemberId());
            String name = member != null && StringUtils.hasText(member.getNickname())
                    ? member.getNickname().trim()
                    : profile.getGameNickname();
            if (StringUtils.hasText(name)) {
                item.setPlayerName(name.trim());
            }
            String gameId = currentGameId(accounts.get(profile.getId()), eventPlayer.getPlatformCode());
            if (StringUtils.hasText(gameId)) {
                item.setGameId(gameId);
            }
        }
    }

    private Map<Long, BasketPlayerProfile> loadProfiles(Collection<Long> playerIds) {
        Map<Long, BasketPlayerProfile> profileMap = new HashMap<>();
        List<BasketPlayerProfile> profiles = playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                .in(BasketPlayerProfile::getId, playerIds)
                .eq(BasketPlayerProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
        for (BasketPlayerProfile profile : profiles) {
            profileMap.put(profile.getId(), profile);
        }
        return profileMap;
    }

    private void overlayPlayers(List<V4PublicEventPlayerItemResp> players) {
        if (CollectionUtils.isEmpty(players)) {
            return;
        }
        Set<Long> playerIds = new HashSet<>();
        for (V4PublicEventPlayerItemResp player : players) {
            if (player != null && player.getSourcePlayerId() != null) {
                playerIds.add(player.getSourcePlayerId());
            }
        }
        if (playerIds.isEmpty()) {
            return;
        }
        Map<Long, BasketPlayerProfile> profileMap = loadProfiles(playerIds);
        Set<Long> memberIds = new HashSet<>();
        for (BasketPlayerProfile profile : profileMap.values()) {
            if (profile.getMemberId() != null) {
                memberIds.add(profile.getMemberId());
            }
        }
        Map<Long, BasketMemberProfile> memberMap = loadMembers(memberIds);
        Map<Long, String> avatarUrls = loadAvatarUrls(memberMap.values());
        Map<Long, List<BasketPlayerPlatformAccount>> accounts = loadAccounts(playerIds);
        for (V4PublicEventPlayerItemResp player : players) {
            if (player == null || player.getSourcePlayerId() == null) {
                continue;
            }
            BasketPlayerProfile profile = profileMap.get(player.getSourcePlayerId());
            if (profile == null) {
                continue;
            }
            BasketMemberProfile member = profile.getMemberId() == null ? null : memberMap.get(profile.getMemberId());
            String name = member != null && StringUtils.hasText(member.getNickname())
                    ? member.getNickname().trim()
                    : profile.getGameNickname();
            if (StringUtils.hasText(name)) {
                player.setPlayerName(name.trim());
            }
            if (StringUtils.hasText(profile.getPositionCode())) {
                player.setPosition(profile.getPositionCode().trim());
            }
            if (member != null && member.getAvatarFileId() != null) {
                player.setAvatarFileId(member.getAvatarFileId());
                player.setAvatarUrl(MemberAvatarSupport.resolve(avatarUrls.get(member.getAvatarFileId())));
            }
            String gameId = currentGameId(accounts.get(profile.getId()), player.getPlatformCode());
            if (StringUtils.hasText(gameId)) {
                player.setGameIdSnapshot(gameId);
            }
        }
    }

    private Map<Long, BasketMemberProfile> loadMembers(Collection<Long> memberIds) {
        Map<Long, BasketMemberProfile> map = new HashMap<>();
        if (CollectionUtils.isEmpty(memberIds)) {
            return map;
        }
        List<BasketMemberProfile> members = memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .in(BasketMemberProfile::getMemberId, memberIds)
                .eq(BasketMemberProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
        for (BasketMemberProfile member : members) {
            map.put(member.getMemberId(), member);
        }
        return map;
    }

    private Map<Long, String> loadAvatarUrls(Collection<BasketMemberProfile> members) {
        Set<Long> fileIds = new HashSet<>();
        for (BasketMemberProfile member : members) {
            if (member.getAvatarFileId() != null) {
                fileIds.add(member.getAvatarFileId());
            }
        }
        return publicSupport.loadAccessUrlMap(fileIds);
    }

    private Map<Long, List<BasketPlayerPlatformAccount>> loadAccounts(Collection<Long> playerIds) {
        Map<Long, List<BasketPlayerPlatformAccount>> grouped = new HashMap<>();
        List<BasketPlayerPlatformAccount> accounts = platformAccountDao.selectList(
                new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                        .in(BasketPlayerPlatformAccount::getPlayerId, playerIds)
                        .eq(BasketPlayerPlatformAccount::getIsDelete, BasketPublicConstants.NOT_DELETED)
                        .eq(BasketPlayerPlatformAccount::getAccountStatus, 1));
        for (BasketPlayerPlatformAccount account : accounts) {
            grouped.computeIfAbsent(account.getPlayerId(), key -> new java.util.ArrayList<>()).add(account);
        }
        return grouped;
    }

    private String currentGameId(List<BasketPlayerPlatformAccount> accounts, String platformCode) {
        if (CollectionUtils.isEmpty(accounts)) {
            return null;
        }
        for (BasketPlayerPlatformAccount account : accounts) {
            if (GameTerminalSupport.matchesDivisionPlatform(platformCode, account.getPlatform())
                    && StringUtils.hasText(account.getDisplayGameId())) {
                return account.getDisplayGameId().trim();
            }
        }
        return null;
    }
}
