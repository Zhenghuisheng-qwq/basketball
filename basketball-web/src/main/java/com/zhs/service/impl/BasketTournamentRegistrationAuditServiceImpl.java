package com.zhs.service.impl;


import com.zhs.model.BasketTournamentRegistrationAudit;
import com.zhs.dao.BasketTournamentRegistrationAuditDao;
import com.zhs.service.IBasketTournamentRegistrationAuditService;
import com.zhs.dto.BasketTournamentRegistrationAuditDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@Slf4j
public class BasketTournamentRegistrationAuditServiceImpl extends ServiceImpl< BasketTournamentRegistrationAuditDao, BasketTournamentRegistrationAudit> implements IBasketTournamentRegistrationAuditService {

    @Resource
    private BasketTournamentRegistrationAuditDao basketTournamentRegistrationAuditDao;

    @Override
    public R addOrUpdate(BasketTournamentRegistrationAuditDto basketTournamentRegistrationAuditDto) {
        if (BeanUtil.isEmpty(basketTournamentRegistrationAuditDto)) throw new ServiceException("数据不能为空");
        BasketTournamentRegistrationAudit basketTournamentRegistrationAudit = new BasketTournamentRegistrationAudit();
        BeanUtil.copyProperties(basketTournamentRegistrationAuditDto,basketTournamentRegistrationAudit);
        if (basketTournamentRegistrationAuditDto.getId() == null){
            basketTournamentRegistrationAuditDao.insert(basketTournamentRegistrationAudit);
            return R.ok("数据插入成功");
        }else {
            basketTournamentRegistrationAuditDao.updateById(basketTournamentRegistrationAudit);
            return R.ok("数据更新成功");
        }
    }
}