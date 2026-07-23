package com.zhs.service.impl;


import com.zhs.model.BasketTeamCaptainHistory;
import com.zhs.dao.BasketTeamCaptainHistoryDao;
import com.zhs.service.IBasketTeamCaptainHistoryService;
import com.zhs.dto.BasketTeamCaptainHistoryDto;


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
public class BasketTeamCaptainHistoryServiceImpl extends ServiceImpl< BasketTeamCaptainHistoryDao, BasketTeamCaptainHistory> implements IBasketTeamCaptainHistoryService {

    @Resource
    private BasketTeamCaptainHistoryDao basketTeamCaptainHistoryDao;

    @Override
    public R addOrUpdate(BasketTeamCaptainHistoryDto basketTeamCaptainHistoryDto) {
        if (BeanUtil.isEmpty(basketTeamCaptainHistoryDto)) throw new ServiceException("数据不能为空");
        BasketTeamCaptainHistory basketTeamCaptainHistory = new BasketTeamCaptainHistory();
        BeanUtil.copyProperties(basketTeamCaptainHistoryDto,basketTeamCaptainHistory);
        if (basketTeamCaptainHistoryDto.getId() == null){
            basketTeamCaptainHistoryDao.insert(basketTeamCaptainHistory);
            return R.ok("数据插入成功");
        }else {
            basketTeamCaptainHistoryDao.updateById(basketTeamCaptainHistory);
            return R.ok("数据更新成功");
        }
    }
}