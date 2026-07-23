package com.zhs.service.impl;


import com.zhs.model.BasketTeamRank;
import com.zhs.dao.BasketTeamRankDao;
import com.zhs.service.IBasketTeamRankService;
import com.zhs.dto.BasketTeamRankDto;


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
public class BasketTeamRankServiceImpl extends ServiceImpl< BasketTeamRankDao, BasketTeamRank> implements IBasketTeamRankService {

    @Resource
    private BasketTeamRankDao basketTeamRankDao;

    @Override
    public R addOrUpdate(BasketTeamRankDto basketTeamRankDto) {
        if (BeanUtil.isEmpty(basketTeamRankDto)) throw new ServiceException("数据不能为空");
        BasketTeamRank basketTeamRank = new BasketTeamRank();
        BeanUtil.copyProperties(basketTeamRankDto,basketTeamRank);
        if (basketTeamRankDto.getId() == null){
            basketTeamRankDao.insert(basketTeamRank);
            return R.ok("数据插入成功");
        }else {
            basketTeamRankDao.updateById(basketTeamRank);
            return R.ok("数据更新成功");
        }
    }
}