package com.zhs.basket.support.v4.roster;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RegistrationSourceEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.support.v4.version.RosterWorkflowSupport;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeamRosterSupport {

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;
    @Autowired
    private IBasketV4RuleVersionService ruleVersionService;

    public boolean isTeamDirect(BasketRegistration registration) {
        if (registration == null) {
            return false;
        }
        if (!RegistrationModeEnum.TEAM.getCode().equals(registration.getRegistrationMode())) {
            return false;
        }
        String source = registration.getRegistrationSource();
        return source == null || RegistrationSourceEnum.TEAM_DIRECT.getCode().equals(source);
    }

    public BasketDivision requireDivision(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    public BasketDivision lockDivision(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = divisionMapper.selectByIdForUpdate(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    public void requireTeamMode(BasketDivision division) {
        if (!RegistrationModeEnum.TEAM.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("仅球队报名 Division 支持最终参赛名单");
        }
    }

    public int formalCap(BasketDivision division) {
        if (division.getMaxTeamCount() == null || division.getMaxTeamCount() <= 0) {
            throw new ServiceException("Division 未配置正式参赛名额");
        }
        return division.getMaxTeamCount();
    }

    public void assertRosterEditable(Long divisionId) {
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(divisionId);
        if (current != null) {
            throw new ServiceException("最终名单已锁定，不能直接修改分配。如需调整请走名单递补并生成新版本");
        }
    }

    public boolean isApproved(BasketRegistration registration) {
        return registration != null
                && RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus());
    }

    public ParticipationRoleEnum roleOf(BasketRegistration registration) {
        return ParticipationRoleEnum.fromCode(registration == null ? null : registration.getParticipationRole());
    }

    public String fingerprint(List<BasketRegistration> registrations) {
        String raw = registrations.stream()
                .filter(this::isApproved)
                .filter(row -> {
                    ParticipationRoleEnum role = roleOf(row);
                    return role == ParticipationRoleEnum.FORMAL
                            || role == ParticipationRoleEnum.CANDIDATE
                            || role == ParticipationRoleEnum.EXCLUDED;
                })
                .sorted(Comparator.comparing(BasketRegistration::getId))
                .map(row -> row.getId() + ":" + roleOf(row).getCode() + ":"
                        + (row.getCandidateOrder() == null ? "" : row.getCandidateOrder()))
                .collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ServiceException("计算名单指纹失败");
        }
    }

    public List<BasketEventTeam> selectFormalEventTeams(Long divisionId) {
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(divisionId);
        RosterWorkflowSupport.assertStructureRosterReady(ruleVersionService.getCurrent(divisionId), current,
                divisionId == null ? null : divisionMapper.selectById(divisionId));
        if (current != null) {
            return eventTeamMapper.selectActiveByDivisionAndRosterVersion(divisionId, current.getId());
        }
        return eventTeamMapper.selectActiveByDivisionId(divisionId);
    }
}
