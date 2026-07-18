package com.zhs.service.impl;


import com.zhs.model.BasketScoreAudit;
import com.zhs.dao.BasketScoreAuditDao;
import com.zhs.service.IBasketScoreAuditService;
import com.zhs.dto.BasketScoreAuditDto;


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
public class BasketScoreAuditServiceImpl extends ServiceImpl< BasketScoreAuditDao, BasketScoreAudit> implements IBasketScoreAuditService {

    @Resource
    private BasketScoreAuditDao basketScoreAuditDao;

    @Override
    public R addOrUpdate(BasketScoreAuditDto basketScoreAuditDto) {
        if (BeanUtil.isEmpty(basketScoreAuditDto)) throw new ServiceException("数据不能为空");
        BasketScoreAudit basketScoreAudit = new BasketScoreAudit();
        BeanUtil.copyProperties(basketScoreAuditDto,basketScoreAudit);
        if (basketScoreAuditDto.getId() == null){
            basketScoreAuditDao.insert(basketScoreAudit);
            return R.ok("数据插入成功");
        }else {
            basketScoreAuditDao.updateById(basketScoreAudit);
            return R.ok("数据更新成功");
        }
    }
}