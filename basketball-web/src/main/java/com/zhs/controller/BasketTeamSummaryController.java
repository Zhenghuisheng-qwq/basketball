package com.zhs.controller;



import com.zhs.service.IBasketTeamSummaryService;
import com.zhs.model.BasketTeamSummary;
import com.zhs.dto.BasketTeamSummaryDto;

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
@RequestMapping("/basket/team/summary")
@Api(value = "")
@Slf4j
public class BasketTeamSummaryController {

    @Resource
    private IBasketTeamSummaryService  iBasketTeamSummaryService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTeamSummary>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTeamSummary> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeamSummary> lambda = new QueryWrapper<BasketTeamSummary>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTeamSummary::getIsDelete,0);
        IPage<BasketTeamSummary> pages =  iBasketTeamSummaryService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTeamSummary> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTeamSummary> wrapper = new QueryWrapper<BasketTeamSummary>()
                            .lambda().eq(BasketTeamSummary::getId,id).eq(BasketTeamSummary::getIsDelete,0);
        return R.ok(iBasketTeamSummaryService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTeamSummary> query = new QueryWrapper<BasketTeamSummary>().lambda().eq(BasketTeamSummary::getId, id).eq(BasketTeamSummary::getIsDelete, 0);
        BasketTeamSummary basketTeamSummary = iBasketTeamSummaryService.getOne(query);
        if(ObjectUtils.isEmpty(basketTeamSummary)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTeamSummary.setIsDelete(1);
        iBasketTeamSummaryService.updateById(basketTeamSummary);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTeamSummaryDto basketTeamSummaryDto){
        if (CollectionUtils.isEmpty(basketTeamSummaryDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTeamSummary> list = new ArrayList<>();
        basketTeamSummaryDto.getIdList().stream().forEach(id ->{
        BasketTeamSummary basketTeamSummary = iBasketTeamSummaryService.getById(id);
            if (ObjectUtils.isEmpty(basketTeamSummary)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTeamSummary.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTeamSummary.setIsDelete(1);
            list.add(basketTeamSummary);
        });
        iBasketTeamSummaryService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTeamSummaryDto basketTeamSummaryDto){
        return iBasketTeamSummaryService.addOrUpdate(basketTeamSummaryDto);
    }

}
