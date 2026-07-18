package com.zhs.controller;



import com.zhs.service.IBasketTournamentRuleService;
import com.zhs.model.BasketTournamentRule;
import com.zhs.dto.BasketTournamentRuleDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/basket/tournament/rule")
@Api(value = "")
@Slf4j
public class BasketTournamentRuleController {

    @Resource
    private IBasketTournamentRuleService  iBasketTournamentRuleService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentRule>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentRule> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentRule> lambda = new QueryWrapper<BasketTournamentRule>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentRule::getIsDelete,0);
        IPage<BasketTournamentRule> pages =  iBasketTournamentRuleService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentRule> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentRule> wrapper = new QueryWrapper<BasketTournamentRule>()
                            .lambda().eq(BasketTournamentRule::getId,id).eq(BasketTournamentRule::getIsDelete,0);
        return R.ok(iBasketTournamentRuleService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentRule> query = new QueryWrapper<BasketTournamentRule>().lambda().eq(BasketTournamentRule::getId, id).eq(BasketTournamentRule::getIsDelete, 0);
        BasketTournamentRule basketTournamentRule = iBasketTournamentRuleService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentRule)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentRule.setIsDelete(1);
        iBasketTournamentRuleService.updateById(basketTournamentRule);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentRuleDto basketTournamentRuleDto){
        if (CollectionUtils.isEmpty(basketTournamentRuleDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentRule> list = new ArrayList<>();
        basketTournamentRuleDto.getIdList().stream().forEach(id ->{
        BasketTournamentRule basketTournamentRule = iBasketTournamentRuleService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentRule)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentRule.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentRule.setIsDelete(1);
            list.add(basketTournamentRule);
        });
        iBasketTournamentRuleService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentRuleDto basketTournamentRuleDto){
        return iBasketTournamentRuleService.addOrUpdate(basketTournamentRuleDto);
    }

}
