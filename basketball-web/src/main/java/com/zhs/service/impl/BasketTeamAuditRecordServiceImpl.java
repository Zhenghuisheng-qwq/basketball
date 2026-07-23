package com.zhs.service.impl;


import com.zhs.model.BasketTeamAuditRecord;
import com.zhs.dao.BasketTeamAuditRecordDao;
import com.zhs.service.IBasketTeamAuditRecordService;
import com.zhs.dto.BasketTeamAuditRecordDto;


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
public class BasketTeamAuditRecordServiceImpl extends ServiceImpl< BasketTeamAuditRecordDao, BasketTeamAuditRecord> implements IBasketTeamAuditRecordService {

    @Resource
    private BasketTeamAuditRecordDao basketTeamAuditRecordDao;

    @Override
    public R addOrUpdate(BasketTeamAuditRecordDto basketTeamAuditRecordDto) {
        if (BeanUtil.isEmpty(basketTeamAuditRecordDto)) throw new ServiceException("数据不能为空");
        BasketTeamAuditRecord basketTeamAuditRecord = new BasketTeamAuditRecord();
        BeanUtil.copyProperties(basketTeamAuditRecordDto,basketTeamAuditRecord);
        if (basketTeamAuditRecordDto.getId() == null){
            basketTeamAuditRecordDao.insert(basketTeamAuditRecord);
            return R.ok("数据插入成功");
        }else {
            basketTeamAuditRecordDao.updateById(basketTeamAuditRecord);
            return R.ok("数据更新成功");
        }
    }
}