package com.zhs.service.impl;


import com.zhs.model.BasketMemberProfile;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.service.IBasketMemberProfileService;
import com.zhs.dto.BasketMemberProfileDto;


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
public class BasketMemberProfileServiceImpl extends ServiceImpl< BasketMemberProfileDao, BasketMemberProfile> implements IBasketMemberProfileService {

    @Resource
    private BasketMemberProfileDao basketMemberProfileDao;

    @Override
    public R addOrUpdate(BasketMemberProfileDto basketMemberProfileDto) {
        if (BeanUtil.isEmpty(basketMemberProfileDto)) throw new ServiceException("数据不能为空");
        BasketMemberProfile basketMemberProfile = new BasketMemberProfile();
        BeanUtil.copyProperties(basketMemberProfileDto,basketMemberProfile);
        if (basketMemberProfileDto.getId() == null){
            basketMemberProfileDao.insert(basketMemberProfile);
            return R.ok("数据插入成功");
        }else {
            basketMemberProfileDao.updateById(basketMemberProfile);
            return R.ok("数据更新成功");
        }
    }
}