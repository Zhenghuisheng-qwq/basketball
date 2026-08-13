package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.ParticipantResolutionStatusEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * GROUP_CROSS placement only. Does not read Standing / Qualification / Registration.
 * Mirrored groups + in-group rank cross. Odd groupCount or advancePerGroup &gt; 2 require MANUAL.
 */
@Component
public class GroupCrossPlacementStrategy {

    public BracketSlotPlan place(GroupCrossPlacementRequest request) {
        if (request == null || request.getGroups() == null || request.getGroups().isEmpty()) {
            throw new ServiceException("GROUP_CROSS 需要已锁定的小组布局");
        }
        if (request.getSourceStageId() == null) {
            throw new ServiceException("GROUP_CROSS 需要 source_stage_id");
        }
        int advancePerGroup = request.getAdvancePerGroup();
        if (advancePerGroup < 1) {
            throw new ServiceException("每组晋级数至少为 1");
        }
        if (advancePerGroup > 2) {
            throw new ServiceException("当前 GROUP_CROSS 仅支持每组晋级 1 或 2 名，其他配置请使用 MANUAL Placement");
        }
        List<GroupLayout> groups = sortedGroups(request.getGroups());
        if (groups.size() < 2) {
            throw new ServiceException("GROUP_CROSS 至少需要 2 个小组");
        }
        if (groups.size() % 2 != 0) {
            throw new ServiceException("V1.3 未定义奇数组自动交叉，GROUP_CROSS 要求偶数小组，请改用 MANUAL Placement");
        }

        int qualifierCount = groups.size() * advancePerGroup;
        EliminationCapacity capacity = EliminationCapacity.of(qualifierCount);
        List<SourcePair> pairs = mirroredPairs(groups, advancePerGroup, request.getSourceStageId());
        Set<GroupRankKey> byeKeys = selectByeKeys(pairs, capacity.getByeCount());
        List<SourcePair> firstRound = applyByes(pairs, byeKeys);
        if (firstRound.size() != capacity.getCapacity() / 2) {
            throw new ServiceException("GROUP_CROSS 无法同时满足交叉约束与 BYE 数量，请改用 MANUAL Placement");
        }
        assertNoSameGroupFirstRound(firstRound);

        List<BracketSlot> slots = new ArrayList<>();
        int slotNo = 1;
        Set<GroupRankKey> used = new HashSet<>();
        for (SourcePair pair : firstRound) {
            slots.add(toSlot(slotNo++, pair.home, byeKeys.contains(keyOf(pair.home))));
            if (pair.home != null && !pair.home.bye) {
                if (!used.add(keyOf(pair.home))) {
                    throw new ServiceException("GROUP_RANK 来源重复分配");
                }
            }
            slots.add(toSlot(slotNo++, pair.away, byeKeys.contains(keyOf(pair.away))));
            if (pair.away != null && !pair.away.bye) {
                if (!used.add(keyOf(pair.away))) {
                    throw new ServiceException("GROUP_RANK 来源重复分配");
                }
            }
        }
        if (used.size() != qualifierCount) {
            throw new ServiceException("GROUP_CROSS 晋级来源必须恰好出现一次");
        }
        BracketSlotPlan plan = new BracketSlotPlan();
        plan.setCapacity(capacity);
        plan.setSlots(slots);
        return plan;
    }

    private static List<GroupLayout> sortedGroups(List<GroupLayout> raw) {
        Set<Long> ids = new HashSet<>();
        Set<String> codes = new HashSet<>();
        List<GroupLayout> groups = new ArrayList<>(raw);
        groups.sort(Comparator
                .comparing(GroupLayout::getGroupOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(GroupLayout::getGroupCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(GroupLayout::getGroupId, Comparator.nullsLast(Long::compareTo)));
        for (GroupLayout group : groups) {
            if (group.getGroupId() == null || group.getGroupCode() == null || group.getGroupCode().isBlank()) {
                throw new ServiceException("小组缺少稳定 groupId / groupCode");
            }
            if (!ids.add(group.getGroupId())) {
                throw new ServiceException("小组 ID 重复");
            }
            if (!codes.add(group.getGroupCode())) {
                throw new ServiceException("小组编码重复");
            }
        }
        return groups;
    }

    private static List<SourcePair> mirroredPairs(List<GroupLayout> groups, int advancePerGroup, Long sourceStageId) {
        List<SourcePair> pairs = new ArrayList<>();
        int groupCount = groups.size();
        for (int i = 0; i < groupCount / 2; i++) {
            GroupLayout left = groups.get(i);
            GroupLayout right = groups.get(groupCount - 1 - i);
            for (int rank = 1; rank <= advancePerGroup; rank++) {
                int crossRank = advancePerGroup + 1 - rank;
                pairs.add(new SourcePair(
                        GroupRankSource.of(sourceStageId, left, rank),
                        GroupRankSource.of(sourceStageId, right, crossRank)));
            }
        }
        return pairs;
    }

    /**
     * Rank 1 before Rank 2; same rank by stable group order. Deterministic, no DB order.
     */
    static Set<GroupRankKey> selectByeKeys(List<SourcePair> pairs, int byeCount) {
        List<GroupRankSource> sources = new ArrayList<>();
        for (SourcePair pair : pairs) {
            sources.add(pair.home);
            sources.add(pair.away);
        }
        sources.sort(Comparator
                .comparingInt((GroupRankSource s) -> s.rank)
                .thenComparingInt(s -> s.groupOrder)
                .thenComparing(s -> s.groupCode)
                .thenComparingLong(s -> s.groupId));
        Set<GroupRankKey> byeKeys = new LinkedHashSet<>();
        for (int i = 0; i < byeCount; i++) {
            byeKeys.add(keyOf(sources.get(i)));
        }
        return byeKeys;
    }

    private static List<SourcePair> applyByes(List<SourcePair> pairs, Set<GroupRankKey> byeKeys) {
        List<SourcePair> firstRound = new ArrayList<>();
        List<GroupRankSource> orphans = new ArrayList<>();
        for (SourcePair pair : pairs) {
            boolean homeBye = byeKeys.contains(keyOf(pair.home));
            boolean awayBye = byeKeys.contains(keyOf(pair.away));
            if (homeBye && awayBye) {
                throw new ServiceException("GROUP_CROSS 无法同时满足交叉约束与 BYE 优先级，请改用 MANUAL Placement");
            }
            if (homeBye) {
                firstRound.add(new SourcePair(pair.home, GroupRankSource.bye()));
                orphans.add(pair.away);
            } else if (awayBye) {
                firstRound.add(new SourcePair(pair.away, GroupRankSource.bye()));
                orphans.add(pair.home);
            } else {
                firstRound.add(pair);
            }
        }
        orphans.sort(Comparator
                .comparingInt((GroupRankSource s) -> s.groupOrder)
                .thenComparingInt(s -> s.rank)
                .thenComparing(s -> s.groupCode));
        if (orphans.size() % 2 != 0) {
            throw new ServiceException("GROUP_CROSS 无法同时满足交叉约束与 BYE 数量，请改用 MANUAL Placement");
        }
        for (int i = 0; i < orphans.size() / 2; i++) {
            GroupRankSource a = orphans.get(i);
            GroupRankSource b = orphans.get(orphans.size() - 1 - i);
            if (Objects.equals(a.groupId, b.groupId)) {
                throw new ServiceException("GROUP_CROSS 非 2 次幂无法避免同组首轮对阵，请改用 MANUAL Placement");
            }
            firstRound.add(new SourcePair(a, b));
        }
        return firstRound;
    }

    private static void assertNoSameGroupFirstRound(List<SourcePair> firstRound) {
        for (SourcePair pair : firstRound) {
            if (pair.home != null && pair.away != null && !pair.home.bye && !pair.away.bye
                    && Objects.equals(pair.home.groupId, pair.away.groupId)) {
                throw new ServiceException("GROUP_CROSS 禁止同组首轮对阵，请改用 MANUAL Placement");
            }
        }
    }

    private static BracketSlot toSlot(int slotNo, GroupRankSource source, boolean byeRecipient) {
        if (source == null || source.bye) {
            return BracketSlot.bye(slotNo);
        }
        BracketSlot slot = BracketSlot.groupRank(slotNo, source.sourceStageId, source.groupId,
                source.groupCode, source.rank);
        if (byeRecipient) {
            // Recipient still occupies a GROUP_RANK slot facing a BYE slot, not a fake match.
            slot.setResolutionStatus(ParticipantResolutionStatusEnum.PENDING.getCode());
        }
        return slot;
    }

    private static GroupRankKey keyOf(GroupRankSource source) {
        if (source == null || source.bye) {
            return new GroupRankKey(-1L, -1L, -1);
        }
        return new GroupRankKey(source.sourceStageId, source.groupId, source.rank);
    }

    record GroupRankKey(Long sourceStageId, Long groupId, int rank) {
    }

    static final class GroupRankSource {
        Long sourceStageId;
        Long groupId;
        String groupCode;
        int groupOrder;
        int rank;
        boolean bye;

        static GroupRankSource of(Long sourceStageId, GroupLayout group, int rank) {
            GroupRankSource source = new GroupRankSource();
            source.sourceStageId = sourceStageId;
            source.groupId = group.getGroupId();
            source.groupCode = group.getGroupCode();
            source.groupOrder = group.getGroupOrder() != null ? group.getGroupOrder() : 0;
            source.rank = rank;
            return source;
        }

        static GroupRankSource bye() {
            GroupRankSource source = new GroupRankSource();
            source.bye = true;
            return source;
        }
    }

    static final class SourcePair {
        final GroupRankSource home;
        final GroupRankSource away;

        SourcePair(GroupRankSource home, GroupRankSource away) {
            this.home = home;
            this.away = away;
        }
    }
}
