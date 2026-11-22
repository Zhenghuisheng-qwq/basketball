package com.zhs.support.v4;

import com.zhs.response.v4.publicapi.V4PublicStageStandingScopeResp;
import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 小组榜始终铺开锁定分组里的全部球队。已有成绩的球队保持正式名次，没打过的按积分、净胜分插到对应位置。
 */
public final class StageStandingBoard {

    private StageStandingBoard() {
    }

    public static void overlayPlayedResults(List<V4PublicStageStandingScopeResp> scopes,
                                            Map<Long, V4PublicStandingItemResp> playedByTeam) {
        if (scopes == null || scopes.isEmpty() || playedByTeam == null || playedByTeam.isEmpty()) {
            return;
        }
        for (V4PublicStageStandingScopeResp scope : scopes) {
            if (scope.getRows() == null || scope.getRows().isEmpty()) {
                continue;
            }
            List<V4PublicStandingItemResp> played = new ArrayList<>();
            List<V4PublicStandingItemResp> idle = new ArrayList<>();
            for (V4PublicStandingItemResp row : scope.getRows()) {
                V4PublicStandingItemResp actual = row.getEventTeamId() == null
                        ? null : playedByTeam.get(row.getEventTeamId());
                if (actual == null) {
                    idle.add(row);
                    continue;
                }
                copyPlayed(row, actual);
                played.add(row);
            }
            played.sort(Comparator.comparingInt(row -> row.getRankNo() == null ? Integer.MAX_VALUE : row.getRankNo()));
            List<V4PublicStandingItemResp> ordered = new ArrayList<>(played);
            for (V4PublicStandingItemResp idleRow : idle) {
                int index = ordered.size();
                for (int i = 0; i < ordered.size(); i++) {
                    if (tableBefore(idleRow, ordered.get(i))) {
                        index = i;
                        break;
                    }
                }
                ordered.add(index, idleRow);
            }
            int rank = 1;
            for (V4PublicStandingItemResp row : ordered) {
                row.setRankNo(rank++);
            }
            scope.setRows(ordered);
        }
    }

    private static boolean tableBefore(V4PublicStandingItemResp left, V4PublicStandingItemResp right) {
        int points = Integer.compare(nz(left.getStandingPoints()), nz(right.getStandingPoints()));
        if (points != 0) {
            return points > 0;
        }
        int diff = Integer.compare(nz(left.getPointDiff()), nz(right.getPointDiff()));
        if (diff != 0) {
            return diff > 0;
        }
        return Integer.compare(nz(left.getWinCount()), nz(right.getWinCount())) > 0;
    }

    private static void copyPlayed(V4PublicStandingItemResp row, V4PublicStandingItemResp actual) {
        row.setRankNo(actual.getRankNo());
        row.setWinCount(nz(actual.getWinCount()));
        row.setLoseCount(nz(actual.getLoseCount()));
        row.setPointFor(nz(actual.getPointFor()));
        row.setPointAgainst(nz(actual.getPointAgainst()));
        row.setPointDiff(actual.getPointDiff() != null
                ? actual.getPointDiff()
                : nz(actual.getPointFor()) - nz(actual.getPointAgainst()));
        row.setStandingPoints(nz(actual.getStandingPoints()));
        if (actual.getStageId() != null) {
            row.setStageId(actual.getStageId());
        }
        if (actual.getWinRate() != null) {
            row.setWinRate(actual.getWinRate());
        }
        if (actual.getQualificationStatus() != null) {
            row.setQualificationStatus(actual.getQualificationStatus());
        }
        if (actual.getEventTeamStatus() != null) {
            row.setEventTeamStatus(actual.getEventTeamStatus());
        }
        if (actual.getDisqualified() != null) {
            row.setDisqualified(actual.getDisqualified());
        }
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
