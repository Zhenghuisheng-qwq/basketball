package com.zhs.service.impl;


import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.service.IBasketTeamSeasonProfileService;
import com.zhs.dto.BasketTeamSeasonProfileDto;


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
public class BasketTeamSeasonProfileServiceImpl extends ServiceImpl< BasketTeamSeasonProfileDao, BasketTeamSeasonProfile> implements IBasketTeamSeasonProfileService {

    @Resource
    private BasketTeamSeasonProfileDao basketTeamSeasonProfileDao;

    @Override
    public R addOrUpdate(BasketTeamSeasonProfileDto basketTeamSeasonProfileDto) {
        if (BeanUtil.isEmpty(basketTeamSeasonProfileDto)) throw new ServiceException("数据不能为空");
        BasketTeamSeasonProfile basketTeamSeasonProfile = new BasketTeamSeasonProfile();
        BeanUtil.copyProperties(basketTeamSeasonProfileDto,basketTeamSeasonProfile);
        if (basketTeamSeasonProfileDto.getId() == null){
            basketTeamSeasonProfileDao.insert(basketTeamSeasonProfile);
            return R.ok("数据插入成功");
        }else {
            basketTeamSeasonProfileDao.updateById(basketTeamSeasonProfile);
            return R.ok("数据更新成功");
        }
    }
}