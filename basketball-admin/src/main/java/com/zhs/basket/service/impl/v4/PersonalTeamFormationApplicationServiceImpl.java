package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.*;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.query.v4.BasketPersonalDrawBatchQuery;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.basket.service.v4.IPersonalTeamFormationApplicationService;
import com.zhs.basket.support.v4.PersonalDrawBatchConfirmSpecification;
import com.zhs.basket.support.v4.PersonalDrawCaptainGrantSupport;
import com.zhs.basket.support.v4.PersonalFormationRegistrationFactory;
import com.zhs.basket.support.v4.PersonalRegistrationStatusTransition;
import com.zhs.basket.support.v4.PersonalTeamFormationConfirmSpecification;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.vo.v4.BasketPersonalDrawBatchDetailVo;
import com.zhs.basket.vo.v4.BasketPersonalDrawBatchListVo;
import com.zhs.basket.vo.v4.BasketPersonalDrawTeamVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class PersonalTeamFormationApplicationServiceImpl implements IPersonalTeamFormationApplicationService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketPersonalDrawBatchMapper drawBatchMapper;
    @Autowired
    private BasketPersonalDrawTeamMapper drawTeamMapper;
    @Autowired
    private BasketPersonalDrawAssignmentMapper drawAssignmentMapper;
    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Autowired
    private PersonalDrawBatchConfirmSpecification batchConfirmSpecification;
    @Autowired
    private PersonalTeamFormationConfirmSpecification formationConfirmSpecification;
    @Autowired
    private PersonalFormationRegistrationFactory formationRegistrationFactory;
    @Autowired
    private PersonalRegistrationStatusTransition personalStatusTransition;
    @Autowired
    private IBasketV4EventSnapshotService eventSnapshotService;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private PersonalDrawCaptainGrantSupport captainGrantSupport;

    @Override
    public List<BasketPersonalDrawBatchListVo> selectBatchList(BasketPersonalDrawBatchQuery query) {
        return drawBatchMapper.selectVoList(query);
    }

    @Override
    public BasketPersonalDrawBatchDetailVo selectBatchDetail(Long drawBatchId) {
        BasketPersonalDrawBatch batch = requireBatch(drawBatchId);
        BasketPersonalDrawBatchDetailVo vo = new BasketPersonalDrawBatchDetailVo();
        vo.setDrawBatchId(batch.getId());
        vo.setDivisionId(batch.getDivisionId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setBatchName(batch.getBatchName());
        vo.setBatchVersion(batch.getBatchVersion());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setResultSource(batch.getResultSource());
        vo.setResultRemark(batch.getResultRemark());
        vo.setUnassignedPolicy(batch.getUnassignedPolicy());
        vo.setUnassignedRemark(batch.getUnassignedRemark());
        vo.setConfirmedTime(batch.getConfirmedTime());
        List<BasketPersonalDrawTeamVo> teams = drawTeamMapper.selectVoByBatchId(drawBatchId);
        for (BasketPersonalDrawTeamVo team : teams) {
            team.setAssignments(drawAssignmentMapper.selectVoByTeamId(team.getDrawTeamId()));
        }
        vo.setTeams(teams);

        BasketDivision division = divisionMapper.selectById(batch.getDivisionId());
        if (division != null) {
            vo.setDivisionName(division.getDivisionName());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraftBatch(BasketPersonalDrawBatchCreateReq req) {
        if (req == null || req.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        if (StringUtils.isEmpty(req.getBatchCode()) || StringUtils.isEmpty(req.getBatchName())) {
            throw new ServiceException("批次编码与名称不能为空");
        }
        BasketDivision division = requirePersonalDivision(req.getDivisionId());
        BasketPersonalDrawBatch active = drawBatchMapper.selectActiveByDivision(division.getId());
        if (active != null && !DrawBatchStatusEnum.CANCELLED.getCode().equals(active.getBatchStatus())
                && !DrawBatchStatusEnum.CONVERTED.getCode().equals(active.getBatchStatus())) {
            throw new ServiceException("该 Division 已有进行中的抽签批次");
        }

        int version = drawBatchMapper.selectNextBatchVersion(division.getId());
        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        BasketPersonalDrawBatch batch = new BasketPersonalDrawBatch();
        batch.setDivisionId(division.getId());
        batch.setBatchCode(req.getBatchCode().trim());
        batch.setBatchName(req.getBatchName().trim());
        batch.setBatchVersion(version);
        batch.setBatchStatus(DrawBatchStatusEnum.DRAFT.getCode());
        batch.setActiveUniqueKey("DIVISION:" + division.getId() + ":BATCH:" + version);
        batch.setResultSource(StringUtils.isNotEmpty(req.getResultSource())
                ? req.getResultSource() : DrawResultSourceEnum.OFFLINE_RANDOM.getCode());
        batch.setResultRemark(req.getResultRemark());
        batch.setCreateBy(operatorId);
        batch.setUpdateBy(operatorId);
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        if (drawBatchMapper.insert(batch) != 1) {
            throw new ServiceException("创建抽签批次失败");
        }
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTeam(Long drawBatchId, BasketPersonalDrawTeamCreateReq req) {
        BasketPersonalDrawBatch batch = requireDraftBatch(drawBatchId);
        if (req == null || StringUtils.isEmpty(req.getTemporaryTeamCode()) || StringUtils.isEmpty(req.getTemporaryTeamName())) {
            throw new ServiceException("临时队伍编码与名称不能为空");
        }
        Date now = new Date();
        BasketPersonalDrawTeam team = new BasketPersonalDrawTeam();
        team.setDrawBatchId(batch.getId());
        team.setDivisionId(batch.getDivisionId());
        team.setTemporaryTeamCode(req.getTemporaryTeamCode().trim());
        team.setTemporaryTeamName(req.getTemporaryTeamName().trim());
        team.setExpectedMemberCount(req.getExpectedMemberCount());
        team.setActualMemberCount(0);
        team.setFormationStatus(FormationStatusEnum.DRAFT.getCode());
        team.setCreateTime(now);
        team.setUpdateTime(now);
        team.setIsDelete(NOT_DELETED);
        if (drawTeamMapper.insert(team) != 1) {
            throw new ServiceException("创建临时队伍失败");
        }
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAssignment(Long drawBatchId, Long drawTeamId, BasketPersonalDrawAssignmentCreateReq req) {
        BasketPersonalDrawBatch batch = requireDraftBatch(drawBatchId);
        BasketPersonalDrawTeam team = requireTeam(drawTeamId, batch.getId());
        if (req == null || req.getPersonalRegistrationId() == null) {
            throw new ServiceException("personalRegistrationId 不能为空");
        }
        BasketPersonalRegistration personalReg = personalRegistrationMapper.selectById(req.getPersonalRegistrationId());
        if (personalReg == null || !batch.getDivisionId().equals(personalReg.getDivisionId())) {
            throw new ServiceException("个人报名不存在或不属于该 Division");
        }
        if (!PersonalRegistrationStatusEnum.APPROVED.getCode().equals(personalReg.getRegistrationStatus())) {
            throw new ServiceException("仅已审核通过的个人报名可分配");
        }
        if (personalReg.getAssignedDrawTeamId() != null) {
            throw new ServiceException("该个人报名已分配到其他临时队伍");
        }

        String role = StringUtils.isNotEmpty(req.getAssignmentRole())
                ? req.getAssignmentRole() : AssignmentRoleEnum.MEMBER.getCode();
        if (AssignmentRoleEnum.CAPTAIN.getCode().equals(role)) {
            List<BasketPersonalDrawAssignment> existing = drawAssignmentMapper.selectByTeamId(drawTeamId);
            boolean hasCaptain = existing.stream()
                    .anyMatch(a -> AssignmentRoleEnum.CAPTAIN.getCode().equals(a.getAssignmentRole()));
            if (hasCaptain) {
                throw new ServiceException("该临时队伍已有队长");
            }
        }

        Date now = new Date();
        BasketPersonalDrawAssignment assignment = new BasketPersonalDrawAssignment();
        assignment.setDrawBatchId(batch.getId());
        assignment.setDrawTeamId(team.getId());
        assignment.setDivisionId(batch.getDivisionId());
        assignment.setPersonalRegistrationId(personalReg.getId());
        assignment.setPlayerId(personalReg.getPlayerId());
        assignment.setAssignmentRole(role);
        assignment.setJoinStatus(JoinStatusEnum.WAITING_TEAM_CREATION.getCode());
        assignment.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        assignment.setCreateTime(now);
        assignment.setUpdateTime(now);
        assignment.setIsDelete(NOT_DELETED);
        if (drawAssignmentMapper.insert(assignment) != 1) {
            throw new ServiceException("添加分配失败");
        }

        if (AssignmentRoleEnum.CAPTAIN.getCode().equals(role)) {
            team.setCaptainPersonalRegistrationId(personalReg.getId());
            team.setCaptainPlayerId(personalReg.getPlayerId());
            team.setCaptainUserId(personalReg.getUserId());
            team.setFormationStatus(FormationStatusEnum.CAPTAIN_ASSIGNED.getCode());
            team.setUpdateTime(now);
            drawTeamMapper.updateById(team);
        }
        drawTeamMapper.refreshActualMemberCount(drawTeamId);
        return assignment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAssignment(Long drawBatchId, Long assignmentId) {
        requireDraftBatch(drawBatchId);
        BasketPersonalDrawAssignment assignment = requireAssignment(assignmentId, drawBatchId);
        drawAssignmentMapper.softDelete(assignmentId);
        drawTeamMapper.refreshActualMemberCount(assignment.getDrawTeamId());

        BasketPersonalDrawTeam team = drawTeamMapper.selectById(assignment.getDrawTeamId());
        if (team != null && assignment.getPersonalRegistrationId().equals(team.getCaptainPersonalRegistrationId())) {
            team.setCaptainPersonalRegistrationId(null);
            team.setCaptainPlayerId(null);
            team.setCaptainUserId(null);
            team.setFormationStatus(FormationStatusEnum.DRAFT.getCode());
            team.setUpdateTime(new Date());
            drawTeamMapper.updateById(team);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBatch(Long drawBatchId, BasketPersonalDrawBatchConfirmReq req) {
        BasketPersonalDrawBatch batch = requireBatch(drawBatchId);
        BasketDivision division = requirePersonalDivision(batch.getDivisionId());
        String unassignedPolicy = req != null ? req.getUnassignedPolicy() : null;
        batchConfirmSpecification.validate(batch, division, unassignedPolicy);

        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        batch.setBatchStatus(DrawBatchStatusEnum.CONFIRMED.getCode());
        batch.setUnassignedPolicy(unassignedPolicy);
        batch.setUnassignedRemark(req != null ? req.getUnassignedRemark() : null);
        batch.setConfirmedBy(operatorId);
        batch.setConfirmedTime(now);
        batch.setUpdateBy(operatorId);
        batch.setUpdateTime(now);
        if (drawBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("确认抽签批次失败");
        }

        List<BasketPersonalDrawAssignment> assignments = drawAssignmentMapper.selectByBatchId(drawBatchId);
        for (BasketPersonalDrawAssignment assignment : assignments) {
            BasketPersonalRegistration personalReg = personalRegistrationMapper.selectById(assignment.getPersonalRegistrationId());
            if (personalReg == null) {
                continue;
            }
            personalReg.setAssignedDrawTeamId(assignment.getDrawTeamId());
            personalStatusTransition.transition(personalReg, PersonalRegistrationStatusEnum.ASSIGNED,
                    PersonalAuditActionEnum.ASSIGN, "抽签批次确认分配");
        }

        for (BasketPersonalDrawTeam team : drawTeamMapper.selectByBatchId(drawBatchId)) {
            if (team.getCaptainPlayerId() != null
                    && FormationStatusEnum.DRAFT.getCode().equals(team.getFormationStatus())) {
                team.setFormationStatus(FormationStatusEnum.CAPTAIN_ASSIGNED.getCode());
                team.setUpdateTime(now);
                drawTeamMapper.updateById(team);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBatch(Long drawBatchId) {
        BasketPersonalDrawBatch batch = requireBatch(drawBatchId);
        if (!DrawBatchStatusEnum.DRAFT.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("仅草稿批次可取消");
        }
        batch.setBatchStatus(DrawBatchStatusEnum.CANCELLED.getCode());
        batch.setActiveUniqueKey(null);
        batch.setUpdateBy(SecurityUtils.getUserId());
        batch.setUpdateTime(new Date());
        if (drawBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("取消抽签批次失败");
        }
        captainGrantSupport.revokeGrantsOnBatchCancel(drawBatchId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindLongTermTeam(Long drawTeamId, BasketPersonalFormationBindTeamReq req) {
        if (req == null || req.getLongTermTeamId() == null) {
            throw new ServiceException("longTermTeamId 不能为空");
        }
        BasketPersonalDrawTeam drawTeam = requireTeam(drawTeamId, null);
        BasketPersonalDrawBatch batch = requireBatch(drawTeam.getDrawBatchId());
        if (!DrawBatchStatusEnum.CONFIRMED.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("抽签批次尚未确认");
        }
        if (drawTeam.getConvertedRegistrationId() != null) {
            throw new ServiceException("该临时队伍已完成转换");
        }

        drawTeam.setLongTermTeamId(req.getLongTermTeamId());
        participationSupport.requireValidTeam(req.getLongTermTeamId());

        List<BasketPersonalDrawAssignment> assignments = drawAssignmentMapper.selectByTeamId(drawTeamId);
        int joinedCount = 0;
        Date now = new Date();
        for (BasketPersonalDrawAssignment assignment : assignments) {
            try {
                participationSupport.requireTeamMember(req.getLongTermTeamId(), assignment.getPlayerId());
                assignment.setJoinStatus(JoinStatusEnum.JOINED.getCode());
                joinedCount++;
            } catch (ServiceException ex) {
                assignment.setJoinStatus(JoinStatusEnum.WAITING_INVITE.getCode());
            }
            assignment.setUpdateTime(now);
            drawAssignmentMapper.updateById(assignment);
        }

        BasketDivision division = divisionMapper.selectById(drawTeam.getDivisionId());
        if (joinedCount == assignments.size() && !assignments.isEmpty()) {
            participationSupport.validateLineupSize(division, assignments.size());
            drawTeam.setFormationStatus(FormationStatusEnum.READY_TO_CONFIRM.getCode());
        } else {
            drawTeam.setFormationStatus(FormationStatusEnum.TEAM_FORMING.getCode());
        }
        drawTeam.setUpdateTime(now);
        if (drawTeamMapper.updateById(drawTeam) != 1) {
            throw new ServiceException("绑定长期球队失败");
        }

        captainGrantSupport.ensureGrantOnBindTeam(drawTeam, division);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmFormation(Long drawTeamId) {
        BasketPersonalDrawTeam drawTeam = requireTeam(drawTeamId, null);
        if (drawTeam.getConvertedRegistrationId() != null) {
            return drawTeam.getConvertedRegistrationId();
        }

        BasketPersonalDrawBatch batch = requireBatch(drawTeam.getDrawBatchId());
        BasketDivision division = requirePersonalDivision(drawTeam.getDivisionId());
        formationConfirmSpecification.validate(drawTeam, division);

        division = divisionMapper.selectByIdForUpdate(division.getId());
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        int approvedCount = registrationMapper.countApprovedByDivisionId(division.getId());
        if (division.getMaxTeamCount() != null && approvedCount >= division.getMaxTeamCount()) {
            throw new ServiceException("该 Division 球队名额已满");
        }

        captainGrantSupport.ensureGrantOnBindTeam(drawTeam, division);

        PersonalFormationRegistrationFactory.FormationRegistrationBundle bundle =
                formationRegistrationFactory.build(drawTeam, division, batch);

        BasketRegistration registration = bundle.getRegistration();
        if (registrationMapper.insert(registration) != 1) {
            throw new ServiceException("创建正式报名失败");
        }

        Long registrationId = registration.getId();
        for (BasketRegistrationPlayer player : bundle.getPlayers()) {
            player.setRegistrationId(registrationId);
        }
        if (registrationPlayerMapper.batchInsert(bundle.getPlayers()) != bundle.getPlayers().size()) {
            throw new ServiceException("保存报名阵容失败");
        }

        BasketRegistrationAudit audit = bundle.getAudit();
        audit.setRegistrationId(registrationId);
        audit.setAuditUserId(SecurityUtils.getUserId());
        audit.setAuditUserName(SecurityUtils.getUsername());
        registrationAuditMapper.insert(audit);

        eventSnapshotService.generateFromApprovedRegistration(registration);

        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        drawTeam.setConvertedRegistrationId(registrationId);
        drawTeam.setFormationStatus(FormationStatusEnum.CONVERTED.getCode());
        drawTeam.setConfirmedBy(operatorId);
        drawTeam.setConfirmedTime(now);
        drawTeam.setUpdateTime(now);
        drawTeamMapper.updateById(drawTeam);

        for (BasketPersonalDrawAssignment assignment : bundle.getAssignments()) {
            BasketPersonalRegistration personalReg = personalRegistrationMapper.selectById(assignment.getPersonalRegistrationId());
            if (personalReg == null) {
                continue;
            }
            personalReg.setConvertedRegistrationId(registrationId);
            personalStatusTransition.transition(personalReg, PersonalRegistrationStatusEnum.CONVERTED,
                    PersonalAuditActionEnum.CONVERT, "个人组队转换为正式报名");
        }

        maybeMarkBatchConverted(batch.getId());
        return registrationId;
    }

    private void maybeMarkBatchConverted(Long batchId) {
        List<BasketPersonalDrawTeam> teams = drawTeamMapper.selectByBatchId(batchId);
        boolean allConverted = !teams.isEmpty() && teams.stream()
                .allMatch(t -> FormationStatusEnum.CONVERTED.getCode().equals(t.getFormationStatus()));
        if (!allConverted) {
            return;
        }
        BasketPersonalDrawBatch batch = drawBatchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        batch.setBatchStatus(DrawBatchStatusEnum.CONVERTED.getCode());
        batch.setActiveUniqueKey(null);
        batch.setUpdateTime(new Date());
        drawBatchMapper.updateById(batch);
    }

    private BasketPersonalDrawBatch requireBatch(Long batchId) {
        if (batchId == null) {
            throw new ServiceException("drawBatchId 不能为空");
        }
        BasketPersonalDrawBatch batch = drawBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ServiceException("抽签批次不存在");
        }
        return batch;
    }

    private BasketPersonalDrawBatch requireDraftBatch(Long batchId) {
        BasketPersonalDrawBatch batch = requireBatch(batchId);
        if (!DrawBatchStatusEnum.DRAFT.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("仅草稿批次可编辑");
        }
        return batch;
    }

    private BasketPersonalDrawTeam requireTeam(Long teamId, Long batchId) {
        if (teamId == null) {
            throw new ServiceException("drawTeamId 不能为空");
        }
        BasketPersonalDrawTeam team = drawTeamMapper.selectById(teamId);
        if (team == null) {
            throw new ServiceException("临时队伍不存在");
        }
        if (batchId != null && !batchId.equals(team.getDrawBatchId())) {
            throw new ServiceException("临时队伍不属于该批次");
        }
        return team;
    }

    private BasketPersonalDrawAssignment requireAssignment(Long assignmentId, Long batchId) {
        if (assignmentId == null) {
            throw new ServiceException("assignmentId 不能为空");
        }
        BasketPersonalDrawAssignment assignment = drawAssignmentMapper.selectById(assignmentId);
        if (assignment == null || !batchId.equals(assignment.getDrawBatchId())) {
            throw new ServiceException("分配记录不存在");
        }
        return assignment;
    }

    private BasketDivision requirePersonalDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        if (!RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("该 Division 不支持个人报名");
        }
        return division;
    }
}
