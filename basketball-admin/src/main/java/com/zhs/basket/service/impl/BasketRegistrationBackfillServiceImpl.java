package com.zhs.basket.service.impl;

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
