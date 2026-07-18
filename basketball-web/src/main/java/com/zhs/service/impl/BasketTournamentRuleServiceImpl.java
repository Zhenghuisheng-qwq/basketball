package com.zhs.service.impl;


import com.zhs.model.BasketTournamentRule;
import com.zhs.dao.BasketTournamentRuleDao;
import com.zhs.service.IBasketTournamentRuleService;
import com.zhs.dto.BasketTournamentRuleDto;


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
public class BasketTournamentRuleServiceImpl extends ServiceImpl< BasketTournamentRuleDao, BasketTournamentRule> implements IBasketTournamentRuleService {

    @Resource
    private BasketTournamentRuleDao basketTournamentRuleDao;

    @Override
    public R addOrUpdate(BasketTournamentRuleDto basketTournamentRuleDto) {
        if (BeanUtil.isEmpty(basketTournamentRuleDto)) throw new ServiceException("数据不能为空");
        BasketTournamentRule basketTournamentRule = new BasketTournamentRule();
        BeanUtil.copyProperties(basketTournamentRuleDto,basketTournamentRule);
        if (basketTournamentRuleDto.getId() == null){
            basketTournamentRuleDao.insert(basketTournamentRule);
            return R.ok("数据插入成功");
        }else {
            basketTournamentRuleDao.updateById(basketTournamentRule);
            return R.ok("数据更新成功");
        }
    }
}