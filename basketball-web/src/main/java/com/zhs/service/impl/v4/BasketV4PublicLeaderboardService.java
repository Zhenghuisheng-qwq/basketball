package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketLeaderboardEntryDao;
import com.zhs.dao.v4.BasketLeaderboardPolicyDao;
import com.zhs.dao.v4.BasketLeaderboardRunDao;
import com.zhs.dao.v4.BasketLeaderboardSettingDao;
import com.zhs.dao.v4.BasketLeaderboardSnapshotDao;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketLeaderboardEntry;
import com.zhs.model.v4.BasketLeaderboardPolicy;
import com.zhs.model.v4.BasketLeaderboardRun;
import com.zhs.model.v4.BasketLeaderboardSetting;
import com.zhs.model.v4.BasketLeaderboardSnapshot;
import com.zhs.response.publicapi.PublicHomePlayerLeaderVo;
import com.zhs.response.v4.publicapi.V4PublicLeaderboardResp;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BasketV4PublicLeaderboardService {

    @Resource
    private BasketLeaderboardSnapshotDao snapshotDao;
    @Resource
    private BasketLeaderboardEntryDao entryDao;
    @Resource
    private BasketLeaderboardPolicyDao policyDao;
    @Resource
    private BasketLeaderboardRunDao runDao;
    @Resource
    private BasketLeaderboardSettingDao settingDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private ObjectMapper objectMapper;

    public Long featuredDivisionId() {
        try {
            BasketLeaderboardSetting setting = settingDao.selectById(1L);
            return setting == null ? null : setting.getFeaturedDivisionId();
        } catch (Exception ex) {
            return null;
        }
    }

    public V4PublicLeaderboardResp query(Long divisionId, String scope, String tab, String mode, String sortBy,
                                         Integer page, Integer pageSize, String keyword) {
        return query(divisionId, scope, tab, mode, sortBy, page, pageSize, keyword, null);
    }

    public V4PublicLeaderboardResp query(Long divisionId, String scope, String tab, String mode, String sortBy,
                                         Integer page, Integer pageSize, String keyword, String sortDir) {
        String normalizedScope = scope == null || scope.isBlank() ? "OVERALL" : scope.trim();
        String normalizedTab = "player".equalsIgnoreCase(tab) ? "PLAYER" : "TEAM";
        String normalizedMode = "total".equalsIgnoreCase(mode) ? "total" : "average";
        String metric = sortBy == null || sortBy.isBlank() ? "PTS" : sortBy.trim().toUpperCase();
        int size = 10;
        int current = page == null || page < 1 ? 1 : page;
        V4PublicLeaderboardResp resp = new V4PublicLeaderboardResp();
        resp.setDivisionId(divisionId);
        resp.setScope(normalizedScope);
        resp.setTab(normalizedTab.equals("PLAYER") ? "player" : "team");
        resp.setMode(normalizedMode);
        resp.setSortBy(metric);
        resp.setPage(current);
        resp.setPageSize(size);
        BasketLeaderboardSnapshot snapshot = findPublic(divisionId, normalizedScope);
        if (snapshot == null) {
            resp.setStatus("NOT_PUBLISHED");
            resp.setEmptyReason("NOT_PUBLISHED");
            resp.setStale(latestFailed(divisionId, normalizedScope));
            return resp;
        }
        BasketLeaderboardPolicy policy = policyDao.selectById(snapshot.getPolicyId());
        resp.setStatus(policy != null && "ARCHIVED".equals(policy.getLifecycle()) ? "ARCHIVED" : "PUBLISHED");
        resp.setPolicyVersion(policy == null ? null : policy.getVersionNo());
        resp.setSnapshotId(snapshot.getId());
        resp.setLastCalculatedAt(snapshot.getPublishedAt());
        resp.setStale(latestFailed(divisionId, normalizedScope));
        List<BasketLeaderboardEntry> entries = entryDao.selectList(new LambdaQueryWrapper<BasketLeaderboardEntry>()
                .eq(BasketLeaderboardEntry::getSnapshotId, snapshot.getId())
                .eq(BasketLeaderboardEntry::getSubjectType, normalizedTab)
                .eq(BasketLeaderboardEntry::getEligible, 1));
        JsonNode gates = gates(policy);
        List<BasketLeaderboardEntry> ranked = new ArrayList<>();
        for (BasketLeaderboardEntry entry : entries) {
            if (passesGate(entry, metric, gates)) {
                ranked.add(entry);
            }
        }
        boolean ascending = sortAscending(metric, sortDir);
        if (isLabelMetric(metric)) {
            sortByLabel(ranked, metric, ascending);
        } else {
            ranked.sort(comparator(metric, "average".equals(normalizedMode), ascending));
        }
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setRankNo(i + 1);
        }
        if (keyword != null && !keyword.isBlank()) {
            ranked = filterKeyword(ranked, keyword.trim().toLowerCase());
        }
        resp.setTotalEligible(ranked.size());
        if (ranked.isEmpty()) {
            resp.setEmptyReason("NO_ELIGIBLE_SUBJECTS");
            return resp;
        }
        int from = Math.min((current - 1) * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        resp.setRows(toRows(ranked.subList(from, to)));
        return resp;
    }

    public PublicHomePlayerLeaderVo leader(Long divisionId, String scope, String metric) {
        V4PublicLeaderboardResp board = query(divisionId, scope, "player", "average", metric, 1, 10, null);
        if (board.getRows().isEmpty()) {
            return null;
        }
        V4PublicLeaderboardResp.Row row = board.getRows().get(0);
        PublicHomePlayerLeaderVo vo = new PublicHomePlayerLeaderVo();
        vo.setEventPlayerId(row.getEventPlayerId());
        vo.setSourcePlayerId(row.getSourcePlayerId());
        vo.setPlayerName(row.getPlayerName());
        vo.setAvatarUrl(row.getAvatarUrl());
        vo.setEventTeamId(row.getEventTeamId());
        vo.setTeamName(row.getTeamName());
        vo.setTeamLogoUrl(row.getTeamLogoUrl());
        vo.setMatchCount(row.getGames());
        vo.setValue(switch (metric) {
            case "REB" -> row.getAverageRebounds();
            case "AST" -> row.getAverageAssists();
            default -> row.getAveragePoints();
        });
        return vo;
    }

    private List<V4PublicLeaderboardResp.Row> toRows(List<BasketLeaderboardEntry> entries) {
        Set<Long> teamIds = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        for (BasketLeaderboardEntry entry : entries) {
            if (entry.getEventTeamId() != null) {
                teamIds.add(entry.getEventTeamId());
            }
            if (entry.getEventPlayerId() != null && entry.getEventPlayerId() > 0) {
                playerIds.add(entry.getEventPlayerId());
            }
        }
        Map<Long, BasketEventTeam> teams = loadTeams(teamIds);
        Map<Long, BasketEventPlayer> players = loadPlayers(playerIds);
        Set<Long> fileIds = new HashSet<>();
        for (BasketEventTeam team : teams.values()) {
            if (team.getTeamLogoFileId() != null) {
                fileIds.add(team.getTeamLogoFileId());
            }
        }
        for (BasketEventPlayer player : players.values()) {
            if (player.getAvatarFileId() != null) {
                fileIds.add(player.getAvatarFileId());
            }
        }
        Map<Long, String> urls = publicSupport.loadAccessUrlMap(fileIds);
        List<V4PublicLeaderboardResp.Row> rows = new ArrayList<>();
        for (BasketLeaderboardEntry entry : entries) {
            V4PublicLeaderboardResp.Row row = new V4PublicLeaderboardResp.Row();
            row.setRank(entry.getRankNo());
            row.setEventTeamId(entry.getEventTeamId());
            row.setEventPlayerId(entry.getEventPlayerId() != null && entry.getEventPlayerId() > 0 ? entry.getEventPlayerId() : null);
            row.setGames(entry.getValidGames());
            row.setWins(entry.getWins());
            row.setLosses(entry.getLosses());
            row.setPoints(entry.getPoints());
            row.setRebounds(entry.getRebound());
            row.setAssists(entry.getAssist());
            row.setSteals(entry.getSteal());
            row.setBlocks(entry.getBlock());
            row.setTurnovers(entry.getTurnover());
            row.setAveragePoints(avg(entry.getPoints(), entry.getValidGames()));
            row.setAverageRebounds(avg(entry.getRebound(), entry.getValidGames()));
            row.setAverageAssists(avg(entry.getAssist(), entry.getValidGames()));
            row.setAverageSteals(avg(entry.getSteal(), entry.getValidGames()));
            row.setAverageBlocks(avg(entry.getBlock(), entry.getValidGames()));
            row.setAverageTurnovers(avg(entry.getTurnover(), entry.getValidGames()));
            row.setFieldGoalMade(entry.getFieldGoalMade());
            row.setFieldGoalAttempt(entry.getFieldGoalAttempt());
            row.setThreePointMade(entry.getThreePointMade());
            row.setThreePointAttempt(entry.getThreePointAttempt());
            row.setFreeThrowMade(entry.getFreeThrowMade());
            row.setFreeThrowAttempt(entry.getFreeThrowAttempt());
            BasketEventTeam team = teams.get(entry.getEventTeamId());
            if (team != null) {
                row.setTeamName(team.getTeamNameSnapshot());
                row.setTeamLogoUrl(urls.get(team.getTeamLogoFileId()));
            }
            BasketEventPlayer player = players.get(entry.getEventPlayerId());
            if (player != null) {
                row.setPlayerName(player.getPlayerNameSnapshot());
                row.setSourcePlayerId(player.getSourcePlayerId());
                row.setPosition(player.getPosition());
                row.setAvatarUrl(urls.get(player.getAvatarFileId()));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<BasketLeaderboardEntry> filterKeyword(List<BasketLeaderboardEntry> ranked, String keyword) {
        Set<Long> teamIds = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        for (BasketLeaderboardEntry entry : ranked) {
            if (entry.getEventTeamId() != null) {
                teamIds.add(entry.getEventTeamId());
            }
            if (entry.getEventPlayerId() != null && entry.getEventPlayerId() > 0) {
                playerIds.add(entry.getEventPlayerId());
            }
        }
        Map<Long, BasketEventTeam> teams = loadTeams(teamIds);
        Map<Long, BasketEventPlayer> players = loadPlayers(playerIds);
        List<BasketLeaderboardEntry> filtered = new ArrayList<>();
        for (BasketLeaderboardEntry entry : ranked) {
            BasketEventTeam team = teams.get(entry.getEventTeamId());
            BasketEventPlayer player = players.get(entry.getEventPlayerId());
            String teamName = team == null || team.getTeamNameSnapshot() == null ? "" : team.getTeamNameSnapshot().toLowerCase();
            String playerName = player == null || player.getPlayerNameSnapshot() == null ? "" : player.getPlayerNameSnapshot().toLowerCase();
            String playerId = player == null || player.getSourcePlayerId() == null ? "" : String.valueOf(player.getSourcePlayerId());
            if (teamName.contains(keyword) || playerName.contains(keyword) || playerId.contains(keyword)
                    || String.valueOf(entry.getEventTeamId()).contains(keyword)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private Map<Long, BasketEventTeam> loadTeams(Collection<Long> ids) {
        Map<Long, BasketEventTeam> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (BasketEventTeam team : eventTeamDao.selectBatchIds(ids)) {
            map.put(team.getId(), team);
        }
        return map;
    }

    private Map<Long, BasketEventPlayer> loadPlayers(Collection<Long> ids) {
        Map<Long, BasketEventPlayer> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (BasketEventPlayer player : eventPlayerDao.selectBatchIds(ids)) {
            map.put(player.getId(), player);
        }
        return map;
    }

    private BasketLeaderboardSnapshot findPublic(Long divisionId, String scope) {
        if (divisionId == null) {
            return null;
        }
        try {
            return snapshotDao.selectOne(new LambdaQueryWrapper<BasketLeaderboardSnapshot>()
                    .eq(BasketLeaderboardSnapshot::getDivisionId, divisionId)
                    .eq(BasketLeaderboardSnapshot::getPublicScopeKey, scope)
                    .eq(BasketLeaderboardSnapshot::getIsPublic, 1)
                    .last("limit 1"));
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean latestFailed(Long divisionId, String scope) {
        try {
            BasketLeaderboardRun run = runDao.selectOne(new LambdaQueryWrapper<BasketLeaderboardRun>()
                    .eq(BasketLeaderboardRun::getDivisionId, divisionId)
                    .eq(BasketLeaderboardRun::getScopeKey, scope)
                    .orderByDesc(BasketLeaderboardRun::getId)
                    .last("limit 1"));
            return run != null && "FAILED".equals(run.getState());
        } catch (Exception ex) {
            return false;
        }
    }

    private JsonNode gates(BasketLeaderboardPolicy policy) {
        if (policy == null || policy.getConfigJson() == null) {
            return null;
        }
        try {
            return objectMapper.readTree(policy.getConfigJson()).path("metricGates");
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean passesGate(BasketLeaderboardEntry entry, String metric, JsonNode gates) {
        if (gates == null || !gates.has(metric)) {
            return true;
        }
        JsonNode gate = gates.get(metric);
        if (gate == null || !gate.path("enabled").asBoolean(false)) {
            return true;
        }
        int perGame = gate.path("minimumAttemptsPerAppearance").asInt(0);
        if (perGame <= 0) {
            return true;
        }
        int attempts = switch (metric) {
            case "THREE_PCT" -> n(entry.getThreePointAttempt());
            case "FT_PCT" -> n(entry.getFreeThrowAttempt());
            default -> n(entry.getFieldGoalAttempt());
        };
        return attempts > 0 && attempts >= perGame * Math.max(n(entry.getValidGames()), 0);
    }

    private boolean sortAscending(String metric, String sortDir) {
        if (sortDir != null && !sortDir.isBlank()) {
            return "asc".equalsIgnoreCase(sortDir.trim());
        }
        return "TOV".equals(metric) || isLabelMetric(metric);
    }

    private boolean isLabelMetric(String metric) {
        return "TEAM_NAME".equals(metric) || "PLAYER_NAME".equals(metric) || "POSITION".equals(metric);
    }

    private void sortByLabel(List<BasketLeaderboardEntry> ranked, String metric, boolean ascending) {
        Set<Long> teamIds = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        for (BasketLeaderboardEntry entry : ranked) {
            if (entry.getEventTeamId() != null) {
                teamIds.add(entry.getEventTeamId());
            }
            if (entry.getEventPlayerId() != null && entry.getEventPlayerId() > 0) {
                playerIds.add(entry.getEventPlayerId());
            }
        }
        Map<Long, BasketEventTeam> teams = loadTeams(teamIds);
        Map<Long, BasketEventPlayer> players = loadPlayers(playerIds);
        Collator collator = Collator.getInstance(Locale.CHINA);
        ranked.sort((left, right) -> {
            int compared = compareText(label(left, metric, teams, players), label(right, metric, teams, players), collator, ascending);
            if (compared != 0) {
                return compared;
            }
            long leftId = nLong(left.getEventPlayerId()) > 0 ? left.getEventPlayerId() : nLong(left.getEventTeamId());
            long rightId = nLong(right.getEventPlayerId()) > 0 ? right.getEventPlayerId() : nLong(right.getEventTeamId());
            return Long.compare(leftId, rightId);
        });
    }

    private String label(BasketLeaderboardEntry entry, String metric, Map<Long, BasketEventTeam> teams,
                         Map<Long, BasketEventPlayer> players) {
        if ("PLAYER_NAME".equals(metric) || "POSITION".equals(metric)) {
            BasketEventPlayer player = players.get(entry.getEventPlayerId());
            if (player == null) {
                return "";
            }
            if ("POSITION".equals(metric)) {
                return player.getPosition() == null ? "" : player.getPosition();
            }
            return player.getPlayerNameSnapshot() == null ? "" : player.getPlayerNameSnapshot();
        }
        BasketEventTeam team = teams.get(entry.getEventTeamId());
        return team == null || team.getTeamNameSnapshot() == null ? "" : team.getTeamNameSnapshot();
    }

    private int compareText(String left, String right, Collator collator, boolean ascending) {
        boolean leftBlank = left == null || left.isBlank();
        boolean rightBlank = right == null || right.isBlank();
        if (leftBlank && rightBlank) {
            return 0;
        }
        if (leftBlank) {
            return 1;
        }
        if (rightBlank) {
            return -1;
        }
        int compared = collator.compare(left, right);
        return ascending ? compared : -compared;
    }

    private Comparator<BasketLeaderboardEntry> comparator(String metric, boolean average, boolean ascending) {
        return (left, right) -> {
            BigDecimal lv = value(left, metric, average);
            BigDecimal rv = value(right, metric, average);
            int compared = compareNullable(lv, rv, ascending);
            if (compared != 0) {
                return compared;
            }
            if ("WINS".equals(metric)) {
                int losses = Integer.compare(n(left.getLosses()), n(right.getLosses()));
                if (losses != 0) {
                    return ascending ? -losses : losses;
                }
            }
            if ("GAMES".equals(metric)) {
                int points = Integer.compare(n(left.getPoints()), n(right.getPoints()));
                if (points != 0) {
                    return ascending ? points : -points;
                }
            }
            int games = Integer.compare(n(right.getValidGames()), n(left.getValidGames()));
            if (games != 0) {
                return games;
            }
            int raw = Integer.compare(raw(right, metric), raw(left, metric));
            if (raw != 0) {
                return raw;
            }
            long leftId = nLong(left.getEventPlayerId()) > 0 ? left.getEventPlayerId() : nLong(left.getEventTeamId());
            long rightId = nLong(right.getEventPlayerId()) > 0 ? right.getEventPlayerId() : nLong(right.getEventTeamId());
            return Long.compare(leftId, rightId);
        };
    }

    private BigDecimal value(BasketLeaderboardEntry entry, String metric, boolean average) {
        if ("WIN_RATE".equals(metric)) {
            int games = n(entry.getValidGames());
            if (games <= 0) {
                return null;
            }
            return BigDecimal.valueOf(n(entry.getWins())).divide(BigDecimal.valueOf(games), 6, RoundingMode.HALF_UP);
        }
        if ("GAMES".equals(metric)) {
            return BigDecimal.valueOf(n(entry.getValidGames()));
        }
        if ("WINS".equals(metric)) {
            return BigDecimal.valueOf(n(entry.getWins()));
        }
        if ("LOSSES".equals(metric)) {
            return BigDecimal.valueOf(n(entry.getLosses()));
        }
        if ("FOUL".equals(metric)) {
            return BigDecimal.ZERO;
        }
        if ("FG_PCT".equals(metric) || "THREE_PCT".equals(metric) || "FT_PCT".equals(metric)) {
            int attempts = switch (metric) {
                case "THREE_PCT" -> n(entry.getThreePointAttempt());
                case "FT_PCT" -> n(entry.getFreeThrowAttempt());
                default -> n(entry.getFieldGoalAttempt());
            };
            if (attempts <= 0) {
                return null;
            }
            int made = switch (metric) {
                case "THREE_PCT" -> n(entry.getThreePointMade());
                case "FT_PCT" -> n(entry.getFreeThrowMade());
                default -> n(entry.getFieldGoalMade());
            };
            return BigDecimal.valueOf(made).divide(BigDecimal.valueOf(attempts), 6, RoundingMode.HALF_UP);
        }
        int total = raw(entry, metric);
        if (!average || n(entry.getValidGames()) <= 0) {
            return BigDecimal.valueOf(total);
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(entry.getValidGames()), 6, RoundingMode.HALF_UP);
    }

    private int raw(BasketLeaderboardEntry entry, String metric) {
        return switch (metric) {
            case "REB" -> n(entry.getRebound());
            case "AST" -> n(entry.getAssist());
            case "STL" -> n(entry.getSteal());
            case "BLK" -> n(entry.getBlock());
            case "TOV" -> n(entry.getTurnover());
            case "FG_PCT" -> n(entry.getFieldGoalMade());
            case "THREE_PCT" -> n(entry.getThreePointMade());
            case "FT_PCT" -> n(entry.getFreeThrowMade());
            case "WIN_RATE", "WINS" -> n(entry.getWins());
            case "LOSSES" -> n(entry.getLosses());
            case "GAMES" -> n(entry.getValidGames());
            default -> n(entry.getPoints());
        };
    }

    private int compareNullable(BigDecimal left, BigDecimal right, boolean ascending) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return ascending ? left.compareTo(right) : right.compareTo(left);
    }

    private BigDecimal avg(Integer total, Integer games) {
        int count = n(games);
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(n(total)).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private long nLong(Long value) {
        return value == null ? 0L : value;
    }
}
