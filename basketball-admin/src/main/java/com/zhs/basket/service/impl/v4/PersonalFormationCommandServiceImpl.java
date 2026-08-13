package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.PersonalFormationMoveReq;
import com.zhs.basket.dto.v4.PersonalFormationRenameReq;
import com.zhs.basket.dto.v4.PersonalFormationSwapReq;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.BasketMemberAccountMapper;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.query.v4.BasketPersonalRegistrationQuery;
import com.zhs.basket.service.v4.*;
import com.zhs.basket.support.v4.formation.*;
import com.zhs.basket.support.v4.roster.GameModeRosterRule;
import com.zhs.basket.support.v4.version.RosterWorkflowSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.vo.v4.*;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonalFormationCommandServiceImpl implements IPersonalFormationCommandService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketDivisionMapper divisionMapper;
    @Autowired private BasketSeasonMapper seasonMapper;
    @Autowired private BasketSeriesMapper seriesMapper;
    @Autowired private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired private BasketPersonalDrawBatchMapper drawBatchMapper;
    @Autowired private BasketPersonalDrawTeamMapper drawTeamMapper;
    @Autowired private BasketPersonalDrawAssignmentMapper drawAssignmentMapper;
    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Autowired private BasketDivisionRosterEntryMapper rosterEntryMapper;
    @Autowired private BasketV4MatchMapper matchMapper;
    @Autowired private IBasketV4RuleVersionService ruleVersionService;
    @Autowired private IBasketV4RosterVersionService rosterVersionService;
    @Autowired private IBasketV4EventSnapshotService eventSnapshotService;
    @Autowired private IBasketV4StructureVersionService structureVersionService;
    @Autowired private FormationStrategyFactory formationStrategyFactory;

    @Override
    public PersonalFormationWorkspaceVo getWorkspace(Long divisionId) {
        BasketDivision division = requirePersonalDivision(divisionId);
        return buildWorkspace(division);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generate(Long divisionId, Long operatorId) {
        BasketDivision division = lockPersonalDivision(divisionId);
        RuleVersionView rule = requireVersionedLockedRule(division);
        GameModeRosterRule rosterRule = GameModeRosterRule.of(division.getGameMode());
        assertProtectionCap(division, rosterRule);
        assertCanRewriteHistory(divisionId);

        List<BasketPersonalRegistration> approved = loadApproved(divisionId);
        rosterRule.assertValidPersonCount(approved.size());

        BasketPersonalDrawBatch batch = requireOrCreateEditableBatch(division, operatorId);
        drawAssignmentMapper.softDeleteByBatchId(batch.getId());
        drawTeamMapper.softDeleteByBatchId(batch.getId());
        for (BasketPersonalRegistration row : approved) {
            personalRegistrationMapper.updateAssignedDrawTeamId(row.getId(), null);
        }

        FormationContext context = new FormationContext();
        context.setDivisionId(divisionId);
        context.setRuleVersionId(rule.getRuleVersionId());
        context.setRosterRule(rosterRule);
        context.setValidPlayers(toPlayers(approved));
        FormationPlan plan = formationStrategyFactory.get(BalancedRandomFormationStrategy.TYPE).plan(context);

        Date now = new Date();
        Map<Long, BasketMemberAccount> accounts = loadAccounts(approved);
        for (FormationTeamPlan teamPlan : plan.getTeams()) {
            BasketPersonalDrawTeam team = new BasketPersonalDrawTeam();
            team.setDrawBatchId(batch.getId());
            team.setDivisionId(divisionId);
            team.setTemporaryTeamCode(teamPlan.getDefaultTeamCode());
            team.setTemporaryTeamName(teamPlan.getDefaultTeamName());
            team.setExpectedMemberCount(teamPlan.getPlayers().size());
            team.setActualMemberCount(teamPlan.getPlayers().size());
            team.setFormationStatus(FormationStatusEnum.DRAFT.getCode());
            team.setCreateTime(now);
            team.setUpdateTime(now);
            team.setIsDelete(NOT_DELETED);
            if (drawTeamMapper.insert(team) != 1) {
                throw new ServiceException("\u521b\u5efa\u4e34\u65f6\u961f\u4f0d\u5931\u8d25");
            }
            int order = 1;
            for (FormationPlayer player : teamPlan.getPlayers()) {
                BasketPersonalDrawAssignment assignment = newAssignment(batch.getId(), team.getId(), divisionId, player, order++, now);
                if (drawAssignmentMapper.insert(assignment) != 1) {
                    throw new ServiceException("\u4fdd\u5b58\u7ec4\u961f\u5206\u914d\u5931\u8d25");
                }
                personalRegistrationMapper.updateAssignedDrawTeamId(player.getPersonalRegistrationId(), team.getId());
            }
            refreshCaptain(team.getId(), accounts);
        }
        batch.setBatchStatus(DrawBatchStatusEnum.GENERATED.getCode());
        batch.setResultSource(DrawResultSourceEnum.BALANCED_RANDOM.getCode());
        batch.setRandomSeed(plan.getRandomSeed());
        batch.setUpdateBy(operatorId);
        batch.setUpdateTime(now);
        if (drawBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("\u66f4\u65b0\u7ec4\u961f\u6279\u6b21\u5931\u8d25");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameTeam(Long divisionId, Long drawTeamId, PersonalFormationRenameReq req) {
        BasketDivision division = lockPersonalDivision(divisionId);
        BasketPersonalDrawBatch batch = requireEditableBatch(division.getId());
        BasketPersonalDrawTeam team = requireTeam(drawTeamId, batch.getId());
        if (req == null || req.getTemporaryTeamName() == null || req.getTemporaryTeamName().trim().isEmpty()) {
            throw new ServiceException("\u4e34\u65f6\u961f\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String name = req.getTemporaryTeamName().trim();
        for (BasketPersonalDrawTeam other : drawTeamMapper.selectByBatchId(batch.getId())) {
            if (!other.getId().equals(drawTeamId) && name.equals(other.getTemporaryTeamName())) {
                throw new ServiceException("\u4e34\u65f6\u961f\u540d\u5728\u6279\u6b21\u5185\u5fc5\u987b\u552f\u4e00");
            }
        }
        team.setTemporaryTeamName(name);
        team.setUpdateTime(new Date());
        drawTeamMapper.updateById(team);
        markAdjusting(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveMember(Long divisionId, PersonalFormationMoveReq req) {
        BasketDivision division = lockPersonalDivision(divisionId);
        BasketPersonalDrawBatch batch = requireEditableBatch(division.getId());
        if (req == null || req.getPersonalRegistrationId() == null) {
            throw new ServiceException("personalRegistrationId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        BasketPersonalRegistration personal = requireApprovedInDivision(req.getPersonalRegistrationId(), divisionId);
        BasketPersonalDrawAssignment assignment = drawAssignmentMapper.selectByPersonalRegistrationId(personal.getId());
        Long fromId = req.getFromDrawTeamId();
        Long toId = req.getToDrawTeamId();
        if (toId != null) {
            requireTeam(toId, batch.getId());
        }
        Date now = new Date();
        if (assignment == null) {
            if (toId == null) {
                return;
            }
            FormationPlayer player = toPlayer(personal);
            BasketPersonalDrawAssignment created = newAssignment(batch.getId(), toId, divisionId, player, 0, now);
            drawAssignmentMapper.insert(created);
            personalRegistrationMapper.updateAssignedDrawTeamId(personal.getId(), toId);
        } else {
            if (!batch.getId().equals(assignment.getDrawBatchId())) {
                throw new ServiceException("\u5206\u914d\u8bb0\u5f55\u4e0d\u5c5e\u4e8e\u5f53\u524d\u6279\u6b21");
            }
            if (fromId != null && !fromId.equals(assignment.getDrawTeamId())) {
                throw new ServiceException("\u6210\u5458\u4e0d\u5728\u6307\u5b9a\u7684\u539f\u961f\u4f0d");
            }
            Long oldTeamId = assignment.getDrawTeamId();
            if (toId == null) {
                drawAssignmentMapper.softDelete(assignment.getId());
                personalRegistrationMapper.updateAssignedDrawTeamId(personal.getId(), null);
            } else {
                assignment.setDrawTeamId(toId);
                assignment.setUpdateTime(now);
                drawAssignmentMapper.updateById(assignment);
                personalRegistrationMapper.updateAssignedDrawTeamId(personal.getId(), toId);
            }
            if (oldTeamId != null) {
                drawTeamMapper.refreshActualMemberCount(oldTeamId);
            }
        }
        if (toId != null) {
            drawTeamMapper.refreshActualMemberCount(toId);
        }
        Map<Long, BasketMemberAccount> accounts = loadAccounts(loadApproved(divisionId));
        for (BasketPersonalDrawTeam team : drawTeamMapper.selectByBatchId(batch.getId())) {
            refreshCaptain(team.getId(), accounts);
        }
        markAdjusting(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void swapMembers(Long divisionId, PersonalFormationSwapReq req) {
        if (req == null || req.getLeftPersonalRegistrationId() == null || req.getRightPersonalRegistrationId() == null) {
            throw new ServiceException("\u4ea4\u6362\u53cc\u65b9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (req.getLeftPersonalRegistrationId().equals(req.getRightPersonalRegistrationId())) {
            throw new ServiceException("\u4e0d\u80fd\u4ea4\u6362\u540c\u4e00\u6210\u5458");
        }
        BasketDivision division = lockPersonalDivision(divisionId);
        BasketPersonalDrawBatch batch = requireEditableBatch(division.getId());
        BasketPersonalDrawAssignment left = requireAssignmentInBatch(req.getLeftPersonalRegistrationId(), batch.getId());
        BasketPersonalDrawAssignment right = requireAssignmentInBatch(req.getRightPersonalRegistrationId(), batch.getId());
        Long leftTeam = left.getDrawTeamId();
        Long rightTeam = right.getDrawTeamId();
        left.setDrawTeamId(rightTeam);
        right.setDrawTeamId(leftTeam);
        Date now = new Date();
        left.setUpdateTime(now);
        right.setUpdateTime(now);
        drawAssignmentMapper.updateById(left);
        drawAssignmentMapper.updateById(right);
        personalRegistrationMapper.updateAssignedDrawTeamId(left.getPersonalRegistrationId(), rightTeam);
        personalRegistrationMapper.updateAssignedDrawTeamId(right.getPersonalRegistrationId(), leftTeam);
        drawTeamMapper.refreshActualMemberCount(leftTeam);
        drawTeamMapper.refreshActualMemberCount(rightTeam);
        Map<Long, BasketMemberAccount> accounts = loadAccounts(loadApproved(divisionId));
        refreshCaptain(leftTeam, accounts);
        refreshCaptain(rightTeam, accounts);
        markAdjusting(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRosterLockResultVo lock(Long divisionId, Long operatorId) {
        BasketDivision division = lockPersonalDivision(divisionId);
        RuleVersionView rule = requireVersionedLockedRule(division);
        GameModeRosterRule rosterRule = GameModeRosterRule.of(division.getGameMode());
        assertProtectionCap(division, rosterRule);
        BasketPersonalDrawBatch batch = requireCurrentBatch(divisionId);
        List<BasketPersonalRegistration> approved = loadApproved(divisionId);
        List<BasketPersonalDrawTeam> teams = drawTeamMapper.selectByBatchId(batch.getId());
        Map<Long, List<BasketPersonalDrawAssignment>> assignmentsByTeam = assignmentsByTeam(batch.getId());
        Map<Long, BasketMemberAccount> accounts = loadAccounts(approved);
        for (BasketPersonalDrawTeam team : teams) {
            refreshCaptain(team.getId(), accounts);
        }
        teams = drawTeamMapper.selectByBatchId(batch.getId());
        validateLock(division, rosterRule, batch, approved, teams, assignmentsByTeam, accounts);

        String fingerprint = fingerprint(teams, assignmentsByTeam);
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(divisionId);
        if (current != null && fingerprint.equals(current.getAllocationFingerprint())) {
            return toLockResult(current, true);
        }
        if (DrawBatchStatusEnum.LOCKED.getCode().equals(batch.getBatchStatus())) {
            if (current != null) {
                return toLockResult(current, true);
            }
            throw new ServiceException("\u7ec4\u961f\u6279\u6b21\u5df2\u9501\u5b9a");
        }
        if (current != null && !matchMapper.selectFinishedPublishedByDivisionId(divisionId).isEmpty()) {
            throw new ServiceException("\u8be5\u8d5b\u4e8b\u5df2\u5b58\u5728\u6b63\u5f0f\u6bd4\u8d5b\u7ed3\u679c\uff0c\u4e0d\u80fd\u91cd\u65b0\u7ec4\u961f\u8986\u76d6\u5386\u53f2");
        }

        BasketDivisionRosterVersion version;
        try {
            version = rosterVersionService.createVersion(divisionId, operatorId);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("\u540d\u5355\u7248\u672c\u521b\u5efa\u51b2\u7a81\uff0c\u8bf7\u91cd\u8bd5");
        }

        Map<Long, BasketPersonalRegistration> registrationById = approved.stream()
                .collect(Collectors.toMap(BasketPersonalRegistration::getId, row -> row));
        Date now = new Date();
        List<BasketDivisionRosterEntry> entries = new ArrayList<>();
        for (BasketPersonalDrawTeam team : teams) {
            List<BasketPersonalDrawAssignment> members = assignmentsByTeam.getOrDefault(team.getId(), List.of());
            BasketEventTeam eventTeam = eventSnapshotService.createTemporaryEventTeam(
                    division, version, team, members, registrationById, team.getCaptainPlayerId(), team.getCaptainUserId());
            BasketDivisionRosterEntry entry = new BasketDivisionRosterEntry();
            entry.setRosterVersionId(version.getId());
            entry.setDivisionId(divisionId);
            entry.setRegistrationId(null);
            entry.setTeamId(null);
            entry.setEventTeamId(eventTeam.getId());
            entry.setParticipationRole(ParticipationRoleEnum.FORMAL.getCode());
            entry.setCandidateOrder(null);
            entry.setCreateTime(now);
            entry.setUpdateTime(now);
            entry.setIsDelete(NOT_DELETED);
            entries.add(entry);
        }
        if (!entries.isEmpty() && rosterEntryMapper.batchInsert(entries) != entries.size()) {
            throw new ServiceException("\u5199\u5165\u540d\u5355\u7248\u672c\u6761\u76ee\u5931\u8d25");
        }

        version.setFormalCount(teams.size());
        version.setCandidateCount(0);
        version.setExcludedCount(0);
        version.setAllocationFingerprint(fingerprint);
        if (rosterVersionMapper.updateById(version) != 1) {
            throw new ServiceException("\u66f4\u65b0\u540d\u5355\u7248\u672c\u7edf\u8ba1\u5931\u8d25");
        }
        if (drawBatchMapper.casLock(batch.getId(), version.getId(), operatorId) != 1) {
            throw new ServiceException("\u9501\u5b9a\u7ec4\u961f\u6279\u6b21\u51b2\u7a81\uff0c\u8bf7\u91cd\u8bd5");
        }
        rosterVersionService.lockVersion(version.getId(), operatorId);
        if (current != null) {
            structureVersionService.invalidateByRosterVersion(current.getId());
        }
        return toLockResult(rosterVersionService.getById(version.getId()), false);
    }

    private PersonalFormationWorkspaceVo buildWorkspace(BasketDivision division) {
        PersonalFormationWorkspaceVo vo = new PersonalFormationWorkspaceVo();
        vo.setDivisionId(division.getId());
        vo.setDivisionName(division.getDivisionName());
        vo.setSeasonId(division.getSeasonId());
        vo.setGameMode(division.getGameMode());
        vo.setRegistrationMode(division.getRegistrationMode());
        GameModeRosterRule rosterRule = GameModeRosterRule.of(division.getGameMode());
        vo.setMinTeamSize(rosterRule.getMinTeamSize());
        vo.setMaxTeamSize(rosterRule.getMaxTeamSize());
        vo.setMinimumValidPersonCount(rosterRule.getMinimumValidPersonCount());
        RuleVersionView rule = ruleVersionService.getCurrent(division.getId());
        vo.setRuleVersion(rule);
        vo.setRuleStatus(rule == null ? "NONE" : rule.getRuleVersionStatus());
        BasketSeason season = seasonMapper.selectById(division.getSeasonId());
        if (season != null) {
            vo.setSeasonTitle(season.getDisplayTitle());
            BasketSeries series = seriesMapper.selectById(season.getSeriesId());
            if (series != null) {
                vo.setSeriesCode(series.getSeriesCode());
                vo.setSeriesName(series.getSeriesName());
            }
        }
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(division.getId());
        if (current != null) {
            vo.setRosterStatus(RosterVersionStatusEnum.LOCKED.getCode());
            vo.setCurrentRosterVersionId(current.getId());
            vo.setCurrentRosterVersionNo(current.getVersionNo());
        } else {
            vo.setRosterStatus("NONE");
        }
        List<BasketPersonalRegistration> approved = loadApproved(division.getId());
        vo.setValidPersonCount(approved.size());
        Map<Long, String> names = loadPlayerNames(division.getId());
        Map<Long, BasketMemberAccount> accounts = loadAccounts(approved);
        BasketPersonalDrawBatch batch = drawBatchMapper.selectActiveByDivision(division.getId());
        Set<Long> assigned = new HashSet<>();
        if (batch != null) {
            vo.setCurrentBatchId(batch.getId());
            vo.setFormationStatus(batch.getBatchStatus());
            List<BasketPersonalDrawTeam> teams = drawTeamMapper.selectByBatchId(batch.getId());
            vo.setTemporaryTeamCount(teams.size());
            Map<Long, List<BasketPersonalDrawAssignment>> byTeam = assignmentsByTeam(batch.getId());
            for (BasketPersonalDrawTeam team : teams) {
                PersonalFormationTeamCardVo card = new PersonalFormationTeamCardVo();
                card.setDrawTeamId(team.getId());
                card.setTemporaryTeamCode(team.getTemporaryTeamCode());
                card.setTemporaryTeamName(team.getTemporaryTeamName());
                card.setCaptainPersonalRegistrationId(team.getCaptainPersonalRegistrationId());
                card.setCaptainPlayerId(team.getCaptainPlayerId());
                card.setCaptainUserId(team.getCaptainUserId());
                List<BasketPersonalDrawAssignment> members = byTeam.getOrDefault(team.getId(), List.of());
                card.setMemberCount(members.size());
                for (BasketPersonalDrawAssignment assignment : members) {
                    assigned.add(assignment.getPersonalRegistrationId());
                    card.getMembers().add(toPersonVo(assignment, names, accounts));
                    if (assignment.getPersonalRegistrationId().equals(team.getCaptainPersonalRegistrationId())) {
                        card.setCaptainPlayerName(names.get(assignment.getPersonalRegistrationId()));
                    }
                }
                vo.getTeams().add(card);
            }
            boolean locked = DrawBatchStatusEnum.LOCKED.getCode().equals(batch.getBatchStatus())
                    || DrawBatchStatusEnum.CONVERTED.getCode().equals(batch.getBatchStatus());
            vo.setCanAdjust(!locked && RosterWorkflowSupport.isVersioned(rule));
        } else {
            vo.setFormationStatus("NONE");
            vo.setTemporaryTeamCount(0);
            vo.setCanAdjust(false);
        }
        for (BasketPersonalRegistration row : approved) {
            if (!assigned.contains(row.getId())) {
                vo.getUnassignedList().add(toPersonVo(row, names, accounts));
            }
        }
        vo.setUnassignedCount(vo.getUnassignedList().size());
        boolean ruleLocked = rule != null && rule.isLocked();
        boolean versioned = RosterWorkflowSupport.isVersioned(rule);
        boolean rosterLocked = current != null;
        boolean batchLocked = batch != null && (DrawBatchStatusEnum.LOCKED.getCode().equals(batch.getBatchStatus())
                || DrawBatchStatusEnum.CONVERTED.getCode().equals(batch.getBatchStatus()));
        boolean hasFinishedMatches = !matchMapper.selectFinishedPublishedByDivisionId(division.getId()).isEmpty();
        vo.setCanGenerate(versioned && ruleLocked && !(rosterLocked && hasFinishedMatches));
        vo.setCanLock(versioned && ruleLocked && batch != null && !batchLocked && vo.getUnassignedCount() == 0
                && vo.getTemporaryTeamCount() >= 2);
        if (batchLocked) {
            vo.setCanAdjust(false);
            vo.setCanLock(false);
        }
        return vo;
    }

    private void validateLock(BasketDivision division, GameModeRosterRule rosterRule, BasketPersonalDrawBatch batch,
                              List<BasketPersonalRegistration> approved, List<BasketPersonalDrawTeam> teams,
                              Map<Long, List<BasketPersonalDrawAssignment>> assignmentsByTeam,
                              Map<Long, BasketMemberAccount> accounts) {
        if (DrawBatchStatusEnum.LOCKED.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("\u7ec4\u961f\u6279\u6b21\u5df2\u9501\u5b9a");
        }
        rosterRule.assertValidPersonCount(approved.size());
        if (teams.size() < 2) {
            throw new ServiceException("\u81f3\u5c11\u9700\u8981 2 \u652f\u4e34\u65f6\u961f");
        }
        Set<Long> seen = new HashSet<>();
        int min = Integer.MAX_VALUE;
        int max = 0;
        Set<String> names = new HashSet<>();
        for (BasketPersonalDrawTeam team : teams) {
            if (team.getTemporaryTeamName() == null || team.getTemporaryTeamName().trim().isEmpty()) {
                throw new ServiceException("\u4e34\u65f6\u961f\u540d\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (!names.add(team.getTemporaryTeamName().trim())) {
                throw new ServiceException("\u4e34\u65f6\u961f\u540d\u5728\u6279\u6b21\u5185\u5fc5\u987b\u552f\u4e00");
            }
            List<BasketPersonalDrawAssignment> members = assignmentsByTeam.getOrDefault(team.getId(), List.of());
            int size = members.size();
            if (size < rosterRule.getMinTeamSize() || size > rosterRule.getMaxTeamSize()) {
                throw new ServiceException("\u6bcf\u961f\u4eba\u6570\u5fc5\u987b\u5728 " + rosterRule.getMinTeamSize()
                        + "-" + rosterRule.getMaxTeamSize() + " \u4e4b\u95f4");
            }
            min = Math.min(min, size);
            max = Math.max(max, size);
            if (team.getCaptainPersonalRegistrationId() == null || team.getCaptainPlayerId() == null) {
                throw new ServiceException("\u6bcf\u652f\u961f\u90fd\u5fc5\u987b\u6709\u4e34\u65f6\u961f\u957f");
            }
            boolean captainInTeam = members.stream()
                    .anyMatch(a -> team.getCaptainPersonalRegistrationId().equals(a.getPersonalRegistrationId()));
            if (!captainInTeam) {
                throw new ServiceException("\u4e34\u65f6\u961f\u957f\u5fc5\u987b\u5c5e\u4e8e\u8be5\u961f");
            }
            FormationPlayer expected = TemporaryCaptainResolver.resolve(members.stream().map(this::toPlayer).toList(), accounts);
            if (!team.getCaptainPersonalRegistrationId().equals(expected.getPersonalRegistrationId())) {
                throw new ServiceException("\u4e34\u65f6\u961f\u957f\u5fc5\u987b\u662f\u8be5\u961f user.create_time \u6700\u65e9\u8005");
            }
            for (BasketPersonalDrawAssignment assignment : members) {
                if (!seen.add(assignment.getPersonalRegistrationId())) {
                    throw new ServiceException("\u5b58\u5728\u91cd\u590d\u5206\u914d\u7684\u6709\u6548\u4e2a\u4eba");
                }
            }
        }
        if (max - min > 1) {
            throw new ServiceException("\u961f\u4f0d\u4eba\u6570\u5dee\u4e0d\u80fd\u8d85\u8fc7 1");
        }
        if (seen.size() != approved.size()) {
            throw new ServiceException("\u6240\u6709\u6709\u6548\u4e2a\u4eba\u5fc5\u987b\u4e14\u53ea\u80fd\u8fdb\u5165\u4e00\u4e2a\u4e34\u65f6\u961f\uff0c\u4e0d\u80fd\u6709\u672a\u5206\u914d");
        }
    }

    private String fingerprint(List<BasketPersonalDrawTeam> teams, Map<Long, List<BasketPersonalDrawAssignment>> assignmentsByTeam) {
        List<BasketPersonalDrawTeam> sorted = new ArrayList<>(teams);
        sorted.sort(Comparator.comparing(BasketPersonalDrawTeam::getTemporaryTeamName, Comparator.nullsLast(String::compareTo))
                .thenComparing(BasketPersonalDrawTeam::getId));
        String raw = sorted.stream().map(team -> {
            List<Long> memberIds = assignmentsByTeam.getOrDefault(team.getId(), List.of()).stream()
                    .map(BasketPersonalDrawAssignment::getPersonalRegistrationId)
                    .sorted()
                    .toList();
            return team.getId() + ":" + team.getTemporaryTeamName() + ":" + team.getCaptainUserId() + ":" + memberIds;
        }).collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ServiceException("\u8ba1\u7b97\u540d\u5355\u6307\u7eb9\u5931\u8d25");
        }
    }

    private void refreshCaptain(Long drawTeamId, Map<Long, BasketMemberAccount> accounts) {
        BasketPersonalDrawTeam team = drawTeamMapper.selectById(drawTeamId);
        List<BasketPersonalDrawAssignment> members = drawAssignmentMapper.selectByTeamId(drawTeamId);
        if (team == null) {
            return;
        }
        if (members.isEmpty()) {
            drawTeamMapper.clearCaptain(drawTeamId);
            return;
        }
        FormationPlayer captain = TemporaryCaptainResolver.resolve(members.stream().map(this::toPlayer).toList(), accounts);
        team.setCaptainPersonalRegistrationId(captain.getPersonalRegistrationId());
        team.setCaptainPlayerId(captain.getPlayerId());
        team.setCaptainUserId(captain.getUserId());
        team.setFormationStatus(FormationStatusEnum.CAPTAIN_ASSIGNED.getCode());
        team.setUpdateTime(new Date());
        drawTeamMapper.updateById(team);
        for (BasketPersonalDrawAssignment assignment : members) {
            String role = captain.getPersonalRegistrationId().equals(assignment.getPersonalRegistrationId())
                    ? AssignmentRoleEnum.CAPTAIN.getCode() : AssignmentRoleEnum.MEMBER.getCode();
            if (!role.equals(assignment.getAssignmentRole())) {
                assignment.setAssignmentRole(role);
                assignment.setUpdateTime(new Date());
                drawAssignmentMapper.updateById(assignment);
            }
        }
        drawTeamMapper.refreshActualMemberCount(drawTeamId);
    }

    private BasketPersonalDrawAssignment newAssignment(Long batchId, Long teamId, Long divisionId,
                                                       FormationPlayer player, int order, Date now) {
        BasketPersonalDrawAssignment assignment = new BasketPersonalDrawAssignment();
        assignment.setDrawBatchId(batchId);
        assignment.setDrawTeamId(teamId);
        assignment.setDivisionId(divisionId);
        assignment.setPersonalRegistrationId(player.getPersonalRegistrationId());
        assignment.setPlayerId(player.getPlayerId());
        assignment.setAssignmentRole(AssignmentRoleEnum.MEMBER.getCode());
        assignment.setJoinStatus(JoinStatusEnum.CONFIRMED.getCode());
        assignment.setSortOrder(order);
        assignment.setCreateTime(now);
        assignment.setUpdateTime(now);
        assignment.setIsDelete(NOT_DELETED);
        return assignment;
    }

    private List<BasketPersonalRegistration> loadApproved(Long divisionId) {
        return personalRegistrationMapper.selectApprovedByDivisionId(divisionId);
    }

    private Map<Long, String> loadPlayerNames(Long divisionId) {
        BasketPersonalRegistrationQuery query = new BasketPersonalRegistrationQuery();
        query.setDivisionId(divisionId);
        query.setRegistrationStatus(PersonalRegistrationStatusEnum.APPROVED.getCode());
        Map<Long, String> names = new HashMap<>();
        for (BasketPersonalRegistrationListVo row : personalRegistrationMapper.selectVoList(query)) {
            names.put(row.getPersonalRegistrationId(), row.getPlayerName());
        }
        return names;
    }

    private Map<Long, BasketMemberAccount> loadAccounts(List<BasketPersonalRegistration> approved) {
        List<Long> ids = approved.stream().map(BasketPersonalRegistration::getUserId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<BasketMemberAccount> rows = memberAccountMapper.selectByIds(ids);
        Map<Long, BasketMemberAccount> map = new HashMap<>();
        for (BasketMemberAccount row : rows) {
            map.put(row.getId(), row);
        }
        return map;
    }

    private Map<Long, List<BasketPersonalDrawAssignment>> assignmentsByTeam(Long batchId) {
        return drawAssignmentMapper.selectByBatchId(batchId).stream()
                .collect(Collectors.groupingBy(BasketPersonalDrawAssignment::getDrawTeamId));
    }

    private List<FormationPlayer> toPlayers(List<BasketPersonalRegistration> rows) {
        return rows.stream().map(this::toPlayer).toList();
    }

    private FormationPlayer toPlayer(BasketPersonalRegistration row) {
        FormationPlayer player = new FormationPlayer();
        player.setPersonalRegistrationId(row.getId());
        player.setPlayerId(row.getPlayerId());
        player.setUserId(row.getUserId());
        return player;
    }

    private FormationPlayer toPlayer(BasketPersonalDrawAssignment assignment) {
        FormationPlayer player = new FormationPlayer();
        player.setPersonalRegistrationId(assignment.getPersonalRegistrationId());
        player.setPlayerId(assignment.getPlayerId());
        BasketPersonalRegistration personal = personalRegistrationMapper.selectById(assignment.getPersonalRegistrationId());
        player.setUserId(personal == null ? null : personal.getUserId());
        return player;
    }

    private PersonalFormationPersonVo toPersonVo(BasketPersonalDrawAssignment assignment, Map<Long, String> names,
                                                 Map<Long, BasketMemberAccount> accounts) {
        PersonalFormationPersonVo vo = new PersonalFormationPersonVo();
        vo.setPersonalRegistrationId(assignment.getPersonalRegistrationId());
        vo.setPlayerId(assignment.getPlayerId());
        vo.setPlayerName(names.get(assignment.getPersonalRegistrationId()));
        vo.setDrawTeamId(assignment.getDrawTeamId());
        vo.setAssignmentRole(assignment.getAssignmentRole());
        BasketPersonalRegistration personal = personalRegistrationMapper.selectById(assignment.getPersonalRegistrationId());
        if (personal != null) {
            vo.setUserId(personal.getUserId());
            vo.setPreferredPosition(personal.getPreferredPosition());
            BasketMemberAccount account = accounts.get(personal.getUserId());
            if (account != null) {
                vo.setUserCreateTime(account.getCreateTime());
            }
        }
        return vo;
    }

    private PersonalFormationPersonVo toPersonVo(BasketPersonalRegistration row, Map<Long, String> names,
                                                 Map<Long, BasketMemberAccount> accounts) {
        PersonalFormationPersonVo vo = new PersonalFormationPersonVo();
        vo.setPersonalRegistrationId(row.getId());
        vo.setPlayerId(row.getPlayerId());
        vo.setUserId(row.getUserId());
        vo.setPlayerName(names.get(row.getId()));
        vo.setPreferredPosition(row.getPreferredPosition());
        BasketMemberAccount account = accounts.get(row.getUserId());
        if (account != null) {
            vo.setUserCreateTime(account.getCreateTime());
        }
        return vo;
    }

    private RuleVersionView requireVersionedLockedRule(BasketDivision division) {
        RuleVersionView rule = ruleVersionService.getCurrent(division.getId());
        if (rule == null || !rule.isLocked()) {
            throw new ServiceException("\u89c4\u5219\u7248\u672c\u5c1a\u672a\u9501\u5b9a\uff0c\u4e0d\u80fd\u9501\u5b9a\u6700\u7ec8\u4e34\u65f6\u7403\u961f");
        }
        if (!RosterWorkflowSupport.isVersioned(rule)) {
            throw new ServiceException("\u4ec5 VERSIONED Division \u652f\u6301\u4e2a\u4eba\u7ec4\u961f\u4e0e\u540d\u5355\u9501\u5b9a");
        }
        return rule;
    }

    private void assertProtectionCap(BasketDivision division, GameModeRosterRule rosterRule) {
        if (division.getMaxPersonCount() != null && division.getMaxPersonCount() > 0
                && division.getMaxPersonCount() < rosterRule.getMinimumValidPersonCount()) {
            throw new ServiceException("\u4e2a\u4eba\u62a5\u540d\u4fdd\u62a4\u4e0a\u9650\u4e0d\u80fd\u4f4e\u4e8e " + rosterRule.getMinimumValidPersonCount());
        }
    }

    private void assertCanRewriteHistory(Long divisionId) {
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(divisionId);
        if (current != null && !matchMapper.selectFinishedPublishedByDivisionId(divisionId).isEmpty()) {
            throw new ServiceException("\u8be5\u8d5b\u4e8b\u5df2\u5b58\u5728\u6b63\u5f0f\u6bd4\u8d5b\u7ed3\u679c\uff0c\u4e0d\u80fd\u91cd\u65b0\u7ec4\u961f\u8986\u76d6\u5386\u53f2");
        }
    }

    private BasketPersonalDrawBatch requireOrCreateEditableBatch(BasketDivision division, Long operatorId) {
        BasketPersonalDrawBatch active = drawBatchMapper.selectActiveByDivision(division.getId());
        if (active != null) {
            DrawBatchStatusEnum status = DrawBatchStatusEnum.fromCode(active.getBatchStatus());
            if (status != null && status.isLockedOrConverted()) {
                drawBatchMapper.clearActiveUniqueKey(active.getId());
                int version = drawBatchMapper.selectNextBatchVersion(division.getId());
                return insertBatch(division.getId(), version, operatorId);
            }
            return active;
        }
        return insertBatch(division.getId(), drawBatchMapper.selectNextBatchVersion(division.getId()), operatorId);
    }

    private BasketPersonalDrawBatch insertBatch(Long divisionId, int version, Long operatorId) {
        Date now = new Date();
        BasketPersonalDrawBatch batch = new BasketPersonalDrawBatch();
        batch.setDivisionId(divisionId);
        batch.setBatchCode("PF-" + divisionId + "-V" + version);
        batch.setBatchName("PERSONAL Formation V" + version);
        batch.setBatchVersion(version);
        batch.setBatchStatus(DrawBatchStatusEnum.DRAFT.getCode());
        batch.setActiveUniqueKey("DIVISION:" + divisionId + ":BATCH:" + version);
        batch.setResultSource(DrawResultSourceEnum.BALANCED_RANDOM.getCode());
        batch.setCreateBy(operatorId);
        batch.setUpdateBy(operatorId);
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        if (drawBatchMapper.insert(batch) != 1) {
            throw new ServiceException("\u521b\u5efa\u7ec4\u961f\u6279\u6b21\u5931\u8d25");
        }
        return batch;
    }

    private void markAdjusting(BasketPersonalDrawBatch batch) {
        batch.setBatchStatus(DrawBatchStatusEnum.ADJUSTING.getCode());
        batch.setResultSource(DrawResultSourceEnum.MANUAL_ADJUSTMENT.getCode());
        batch.setUpdateTime(new Date());
        drawBatchMapper.updateById(batch);
    }

    private BasketPersonalDrawBatch requireEditableBatch(Long divisionId) {
        BasketPersonalDrawBatch batch = requireCurrentBatch(divisionId);
        DrawBatchStatusEnum status = DrawBatchStatusEnum.fromCode(batch.getBatchStatus());
        if (status == null || !status.isEditable()) {
            throw new ServiceException("\u7ec4\u961f\u5df2\u9501\u5b9a\uff0c\u4e0d\u53ef\u76f4\u63a5\u4fee\u6539");
        }
        return batch;
    }

    private BasketPersonalDrawBatch requireCurrentBatch(Long divisionId) {
        BasketPersonalDrawBatch batch = drawBatchMapper.selectActiveByDivision(divisionId);
        if (batch == null) {
            throw new ServiceException("\u5f53\u524d\u6ca1\u6709\u7ec4\u961f\u6279\u6b21");
        }
        return batch;
    }

    private BasketPersonalDrawTeam requireTeam(Long teamId, Long batchId) {
        BasketPersonalDrawTeam team = drawTeamMapper.selectById(teamId);
        if (team == null || !batchId.equals(team.getDrawBatchId())) {
            throw new ServiceException("\u4e34\u65f6\u961f\u4f0d\u4e0d\u5b58\u5728");
        }
        return team;
    }

    private BasketPersonalDrawAssignment requireAssignmentInBatch(Long personalRegistrationId, Long batchId) {
        BasketPersonalDrawAssignment assignment = drawAssignmentMapper.selectByPersonalRegistrationId(personalRegistrationId);
        if (assignment == null || !batchId.equals(assignment.getDrawBatchId())) {
            throw new ServiceException("\u6210\u5458\u5c1a\u672a\u5206\u914d\u5230\u4e34\u65f6\u961f");
        }
        return assignment;
    }

    private BasketPersonalRegistration requireApprovedInDivision(Long id, Long divisionId) {
        BasketPersonalRegistration row = personalRegistrationMapper.selectById(id);
        if (row == null || !divisionId.equals(row.getDivisionId())) {
            throw new ServiceException("\u4e2a\u4eba\u62a5\u540d\u4e0d\u5b58\u5728");
        }
        if (!PersonalRegistrationStatusEnum.APPROVED.getCode().equals(row.getRegistrationStatus())) {
            throw new ServiceException("\u4ec5\u5ba1\u6838\u901a\u8fc7\u7684\u6709\u6548\u4e2a\u4eba\u53ef\u8fdb\u5165\u7ec4\u961f");
        }
        return row;
    }

    private BasketDivision requirePersonalDivision(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division \u4e0d\u5b58\u5728");
        }
        if (!RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("\u4ec5\u4e2a\u4eba\u62a5\u540d Division \u652f\u6301 Formation");
        }
        return division;
    }

    private BasketDivision lockPersonalDivision(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        BasketDivision division = divisionMapper.selectByIdForUpdate(divisionId);
        if (division == null) {
            throw new ServiceException("Division \u4e0d\u5b58\u5728");
        }
        if (!RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("\u4ec5\u4e2a\u4eba\u62a5\u540d Division \u652f\u6301 Formation");
        }
        return division;
    }

    private TeamRosterLockResultVo toLockResult(BasketDivisionRosterVersion version, boolean replay) {
        TeamRosterLockResultVo vo = new TeamRosterLockResultVo();
        vo.setRosterVersionId(version.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setStatus(version.getStatus());
        vo.setFormalCount(version.getFormalCount());
        vo.setCandidateCount(version.getCandidateCount());
        vo.setExcludedCount(version.getExcludedCount());
        vo.setIdempotentReplay(replay);
        return vo;
    }
}
