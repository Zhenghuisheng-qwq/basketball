package com.zhs.service.impl;


import com.zhs.model.BasketTournamentAudit;
import com.zhs.dao.BasketTournamentAuditDao;
import com.zhs.service.IBasketTournamentAuditService;
import com.zhs.dto.BasketTournamentAuditDto;


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
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketTournamentAuditServiceImpl extends ServiceImpl< BasketTournamentAuditDao, BasketTournamentAudit> implements IBasketTournamentAuditService {

    @Resource
    private BasketTournamentAuditDao basketTournamentAuditDao;

    @Override
    public R addOrUpdate(BasketTournamentAuditDto basketTournamentAuditDto) {
        if (BeanUtil.isEmpty(basketTournamentAuditDto)) throw new ServiceException("数据不能为空");
        BasketTournamentAudit basketTournamentAudit = new BasketTournamentAudit();
        BeanUtil.copyProperties(basketTournamentAuditDto,basketTournamentAudit);
        if (basketTournamentAuditDto.getId() == null){
            basketTournamentAuditDao.insert(basketTournamentAudit);
            return R.ok("数据插入成功");
        }else {
            basketTournamentAuditDao.updateById(basketTournamentAudit);
            return R.ok("数据更新成功");
        }
    }
}