package com.zhs.controller;



import com.zhs.service.IBasketTournamentService;
import com.zhs.model.BasketTournament;
import com.zhs.dto.BasketTournamentDto;

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
@RequestMapping("/basket/tournament")
@Api(value = "")
@Slf4j
public class BasketTournamentController {

    @Resource
    private IBasketTournamentService  iBasketTournamentService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournament>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournament> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournament> lambda = new QueryWrapper<BasketTournament>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournament::getIsDelete,0);
        IPage<BasketTournament> pages =  iBasketTournamentService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournament> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournament> wrapper = new QueryWrapper<BasketTournament>()
                            .lambda().eq(BasketTournament::getId,id).eq(BasketTournament::getIsDelete,0);
        return R.ok(iBasketTournamentService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournament> query = new QueryWrapper<BasketTournament>().lambda().eq(BasketTournament::getId, id).eq(BasketTournament::getIsDelete, 0);
        BasketTournament basketTournament = iBasketTournamentService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournament)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournament.setIsDelete(1);
        iBasketTournamentService.updateById(basketTournament);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentDto basketTournamentDto){
        if (CollectionUtils.isEmpty(basketTournamentDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournament> list = new ArrayList<>();
        basketTournamentDto.getIdList().stream().forEach(id ->{
        BasketTournament basketTournament = iBasketTournamentService.getById(id);
            if (ObjectUtils.isEmpty(basketTournament)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournament.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournament.setIsDelete(1);
            list.add(basketTournament);
        });
        iBasketTournamentService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentDto basketTournamentDto){
        return iBasketTournamentService.addOrUpdate(basketTournamentDto);
    }

}
