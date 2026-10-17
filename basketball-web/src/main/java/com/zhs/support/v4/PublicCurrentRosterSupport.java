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
import com.zhs.response.v4.publicapi.V4PublicEventPlayerItemResp;
import com.zhs.response.v4.publicapi.V4PublicEventTeamDetailResp;
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
        if (StringUtils.hasText(source.getTeamName())) {
            resp.setTeamName(source.getTeamName().trim());
        }
        if (StringUtils.hasText(source.getShortName())) {
            resp.setTeamShortName(source.getShortName().trim());
        }
        resp.setDescription(source.getDescription());
        if (source.getLogoFileId() == null) {
            return;
        }
        resp.setTeamLogoFileId(source.getLogoFileId());
        Map<Long, String> urls = publicSupport.loadAccessUrlMap(Collections.singleton(source.getLogoFileId()));
        resp.setTeamLogoUrl(urls.get(source.getLogoFileId()));
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
        List<BasketPlayerProfile> profiles = playerProfileDao.selectList(new LambdaQueryWrapper<BasketPlayerProfile>()
                .in(BasketPlayerProfile::getId, playerIds)
                .eq(BasketPlayerProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Map<Long, BasketPlayerProfile> profileMap = new HashMap<>();
        Set<Long> memberIds = new HashSet<>();
        for (BasketPlayerProfile profile : profiles) {
            profileMap.put(profile.getId(), profile);
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
