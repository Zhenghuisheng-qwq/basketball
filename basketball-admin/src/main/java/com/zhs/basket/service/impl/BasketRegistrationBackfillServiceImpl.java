package com.zhs.basket.service.impl;

import com.zhs.basket.mapper.BasketTeamSeasonProfileMapper;
import com.zhs.basket.service.IBasketParticipationGenerationService;
import com.zhs.basket.service.IBasketRegistrationBackfillService;
import com.zhs.basket.mapper.BasketTournamentRegistrationMapper;
import com.zhs.basket.vo.RegistrationBackfillResultVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BasketRegistrationBackfillServiceImpl implements IBasketRegistrationBackfillService {

    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private IBasketParticipationGenerationService participationGenerationService;
    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationBackfillResultVo backfillApprovedRegistrations(Long operatorId) {
        List<Long> registrationIds = registrationMapper.selectApprovedIdsWithoutEventTeam();
        RegistrationBackfillResultVo result = new RegistrationBackfillResultVo();
        result.setScanned(registrationIds.size());
        for (Long registrationId : registrationIds) {
            backfillOne(registrationId, operatorId, result);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationBackfillResultVo backfillRegistration(Long registrationId, Long operatorId) {
        if (registrationId == null) {
            throw new ServiceException("报名ID不能为空");
        }
        RegistrationBackfillResultVo result = new RegistrationBackfillResultVo();
        result.setScanned(1);
        backfillOne(registrationId, operatorId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationBackfillResultVo repairUnlockedApprovedEventTeamRosters(Long operatorId) {
        List<Long> registrationIds = teamSeasonProfileMapper.selectRegistrationIdsWithUnlockedApprovedEventTeam();
        RegistrationBackfillResultVo result = new RegistrationBackfillResultVo();
        result.setScanned(registrationIds.size());
        for (Long registrationId : registrationIds) {
            repairUnlockedOne(registrationId, operatorId, result);
        }
        return result;
    }

    private void repairUnlockedOne(Long registrationId, Long operatorId, RegistrationBackfillResultVo result) {
        try {
            participationGenerationService.ensureRosterLockedForApprovedRegistration(registrationId, operatorId);
            result.setSucceeded(result.getSucceeded() + 1);
            result.getMessages().add("报名 " + registrationId + " 阵容锁定修复成功");
        } catch (ServiceException ex) {
            result.setFailed(result.getFailed() + 1);
            result.getMessages().add("报名 " + registrationId + " 阵容锁定修复失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            result.setFailed(result.getFailed() + 1);
            result.getMessages().add("报名 " + registrationId + " 阵容锁定修复异常：" + ex.getMessage());
        }
    }

    private void backfillOne(Long registrationId, Long operatorId, RegistrationBackfillResultVo result) {
        try {
            participationGenerationService.backfillFromApprovedRegistration(registrationId, operatorId, "管理员触发历史补偿");
            result.setSucceeded(result.getSucceeded() + 1);
            result.getMessages().add("报名 " + registrationId + " 补偿成功");
        } catch (ServiceException ex) {
            result.setFailed(result.getFailed() + 1);
            result.getMessages().add("报名 " + registrationId + " 补偿失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            result.setFailed(result.getFailed() + 1);
            result.getMessages().add("报名 " + registrationId + " 补偿异常：" + ex.getMessage());
        }
    }
}
