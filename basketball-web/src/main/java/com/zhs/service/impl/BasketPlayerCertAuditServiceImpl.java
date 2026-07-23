package com.zhs.service.impl;


import com.zhs.model.BasketPlayerCertAudit;
import com.zhs.dao.BasketPlayerCertAuditDao;
import com.zhs.service.IBasketPlayerCertAuditService;
import com.zhs.dto.BasketPlayerCertAuditDto;


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
public class BasketPlayerCertAuditServiceImpl extends ServiceImpl< BasketPlayerCertAuditDao, BasketPlayerCertAudit> implements IBasketPlayerCertAuditService {

    @Resource
    private BasketPlayerCertAuditDao basketPlayerCertAuditDao;

    @Override
    public R addOrUpdate(BasketPlayerCertAuditDto basketPlayerCertAuditDto) {
        if (BeanUtil.isEmpty(basketPlayerCertAuditDto)) throw new ServiceException("数据不能为空");
        BasketPlayerCertAudit basketPlayerCertAudit = new BasketPlayerCertAudit();
        BeanUtil.copyProperties(basketPlayerCertAuditDto,basketPlayerCertAudit);
        if (basketPlayerCertAuditDto.getId() == null){
            basketPlayerCertAuditDao.insert(basketPlayerCertAudit);
            return R.ok("数据插入成功");
        }else {
            basketPlayerCertAuditDao.updateById(basketPlayerCertAudit);
            return R.ok("数据更新成功");
        }
    }
}