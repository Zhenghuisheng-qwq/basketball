package com.zhs.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.dao.BasketWebUserRoleDao;
import com.zhs.dto.BasketWebUserRoleDto;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketWebUserRole;
import com.zhs.service.IBasketWebUserRoleService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 *
 * @author 
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketWebUserRoleServiceImpl extends ServiceImpl<BasketWebUserRoleDao, BasketWebUserRole> implements IBasketWebUserRoleService {

    @Resource
    private BasketWebUserRoleDao basketWebUserRoleDao;

    @Override
    public R addOrUpdate(BasketWebUserRoleDto basketWebUserRoleDto) {
        if (BeanUtil.isEmpty(basketWebUserRoleDto)) throw new ServiceException("数据不能为空");
        BasketWebUserRole basketWebUserRole = new BasketWebUserRole();
        BeanUtil.copyProperties(basketWebUserRoleDto, basketWebUserRole);
        if (basketWebUserRoleDto.getId() == null) {
            basketWebUserRoleDao.insert(basketWebUserRole);
            return R.ok("数据插入成功");
        } else {
            basketWebUserRoleDao.updateById(basketWebUserRole);
            return R.ok("数据更新成功");
        }
    }
}
