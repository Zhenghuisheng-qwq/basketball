package com.zhs.controller;



import com.zhs.service.IBasketTournamentRoundService;
import com.zhs.model.BasketTournamentRound;
import com.zhs.dto.BasketTournamentRoundDto;

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
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/basket/tournament/round")
@Api(value = "")
@Slf4j
public class BasketTournamentRoundController {

    @Resource
    private IBasketTournamentRoundService  iBasketTournamentRoundService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentRound>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentRound> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentRound> lambda = new QueryWrapper<BasketTournamentRound>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentRound::getIsDelete,0);
        IPage<BasketTournamentRound> pages =  iBasketTournamentRoundService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentRound> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentRound> wrapper = new QueryWrapper<BasketTournamentRound>()
                            .lambda().eq(BasketTournamentRound::getId,id).eq(BasketTournamentRound::getIsDelete,0);
        return R.ok(iBasketTournamentRoundService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentRound> query = new QueryWrapper<BasketTournamentRound>().lambda().eq(BasketTournamentRound::getId, id).eq(BasketTournamentRound::getIsDelete, 0);
        BasketTournamentRound basketTournamentRound = iBasketTournamentRoundService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentRound)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentRound.setIsDelete(1);
        iBasketTournamentRoundService.updateById(basketTournamentRound);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentRoundDto basketTournamentRoundDto){
        if (CollectionUtils.isEmpty(basketTournamentRoundDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentRound> list = new ArrayList<>();
        basketTournamentRoundDto.getIdList().stream().forEach(id ->{
        BasketTournamentRound basketTournamentRound = iBasketTournamentRoundService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentRound)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentRound.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentRound.setIsDelete(1);
            list.add(basketTournamentRound);
        });
        iBasketTournamentRoundService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentRoundDto basketTournamentRoundDto){
        return iBasketTournamentRoundService.addOrUpdate(basketTournamentRoundDto);
    }

}
