package com.zhs.service.impl;


import com.zhs.model.BasketTeamSummary;
import com.zhs.dao.BasketTeamSummaryDao;
import com.zhs.service.IBasketTeamSummaryService;
import com.zhs.dto.BasketTeamSummaryDto;


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
public class BasketTeamSummaryServiceImpl extends ServiceImpl< BasketTeamSummaryDao, BasketTeamSummary> implements IBasketTeamSummaryService {

    @Resource
    private BasketTeamSummaryDao basketTeamSummaryDao;

    @Override
    public R addOrUpdate(BasketTeamSummaryDto basketTeamSummaryDto) {
        if (BeanUtil.isEmpty(basketTeamSummaryDto)) throw new ServiceException("数据不能为空");
        BasketTeamSummary basketTeamSummary = new BasketTeamSummary();
        BeanUtil.copyProperties(basketTeamSummaryDto,basketTeamSummary);
        if (basketTeamSummaryDto.getId() == null){
            basketTeamSummaryDao.insert(basketTeamSummary);
            return R.ok("数据插入成功");
        }else {
            basketTeamSummaryDao.updateById(basketTeamSummary);
            return R.ok("数据更新成功");
        }
    }
}