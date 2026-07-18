package com.zhs.service.impl;


import com.zhs.model.BasketTeamLog;
import com.zhs.dao.BasketTeamLogDao;
import com.zhs.service.IBasketTeamLogService;
import com.zhs.dto.BasketTeamLogDto;


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
public class BasketTeamLogServiceImpl extends ServiceImpl< BasketTeamLogDao, BasketTeamLog> implements IBasketTeamLogService {

    @Resource
    private BasketTeamLogDao basketTeamLogDao;

    @Override
    public R addOrUpdate(BasketTeamLogDto basketTeamLogDto) {
        if (BeanUtil.isEmpty(basketTeamLogDto)) throw new ServiceException("数据不能为空");
        BasketTeamLog basketTeamLog = new BasketTeamLog();
        BeanUtil.copyProperties(basketTeamLogDto,basketTeamLog);
        if (basketTeamLogDto.getId() == null){
            basketTeamLogDao.insert(basketTeamLog);
            return R.ok("数据插入成功");
        }else {
            basketTeamLogDao.updateById(basketTeamLog);
            return R.ok("数据更新成功");
        }
    }
}