package com.zhs.controller;



import com.zhs.service.IBasketMatchRosterService;
import com.zhs.model.BasketMatchRoster;
import com.zhs.dto.BasketMatchRosterDto;

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
@RequestMapping("/basket/match/roster")
@Api(value = "")
@Slf4j
public class BasketMatchRosterController {

    @Resource
    private IBasketMatchRosterService  iBasketMatchRosterService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMatchRoster>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMatchRoster> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatchRoster> lambda = new QueryWrapper<BasketMatchRoster>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMatchRoster::getIsDelete,0);
        IPage<BasketMatchRoster> pages =  iBasketMatchRosterService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMatchRoster> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMatchRoster> wrapper = new QueryWrapper<BasketMatchRoster>()
                            .lambda().eq(BasketMatchRoster::getId,id).eq(BasketMatchRoster::getIsDelete,0);
        return R.ok(iBasketMatchRosterService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMatchRoster> query = new QueryWrapper<BasketMatchRoster>().lambda().eq(BasketMatchRoster::getId, id).eq(BasketMatchRoster::getIsDelete, 0);
        BasketMatchRoster basketMatchRoster = iBasketMatchRosterService.getOne(query);
        if(ObjectUtils.isEmpty(basketMatchRoster)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMatchRoster.setIsDelete(1);
        iBasketMatchRosterService.updateById(basketMatchRoster);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMatchRosterDto basketMatchRosterDto){
        if (CollectionUtils.isEmpty(basketMatchRosterDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMatchRoster> list = new ArrayList<>();
        basketMatchRosterDto.getIdList().stream().forEach(id ->{
        BasketMatchRoster basketMatchRoster = iBasketMatchRosterService.getById(id);
            if (ObjectUtils.isEmpty(basketMatchRoster)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMatchRoster.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMatchRoster.setIsDelete(1);
            list.add(basketMatchRoster);
        });
        iBasketMatchRosterService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMatchRosterDto basketMatchRosterDto){
        return iBasketMatchRosterService.addOrUpdate(basketMatchRosterDto);
    }

}
