package com.zhs.controller;



import com.zhs.service.IBasketTeamCaptainHistoryService;
import com.zhs.model.BasketTeamCaptainHistory;
import com.zhs.dto.BasketTeamCaptainHistoryDto;

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
@RequestMapping("/basket/team/captain/history")
@Api(value = "")
@Slf4j
public class BasketTeamCaptainHistoryController {

    @Resource
    private IBasketTeamCaptainHistoryService  iBasketTeamCaptainHistoryService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTeamCaptainHistory>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTeamCaptainHistory> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeamCaptainHistory> lambda = new QueryWrapper<BasketTeamCaptainHistory>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTeamCaptainHistory::getIsDelete,0);
        IPage<BasketTeamCaptainHistory> pages =  iBasketTeamCaptainHistoryService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTeamCaptainHistory> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTeamCaptainHistory> wrapper = new QueryWrapper<BasketTeamCaptainHistory>()
                            .lambda().eq(BasketTeamCaptainHistory::getId,id).eq(BasketTeamCaptainHistory::getIsDelete,0);
        return R.ok(iBasketTeamCaptainHistoryService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTeamCaptainHistory> query = new QueryWrapper<BasketTeamCaptainHistory>().lambda().eq(BasketTeamCaptainHistory::getId, id).eq(BasketTeamCaptainHistory::getIsDelete, 0);
        BasketTeamCaptainHistory basketTeamCaptainHistory = iBasketTeamCaptainHistoryService.getOne(query);
        if(ObjectUtils.isEmpty(basketTeamCaptainHistory)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTeamCaptainHistory.setIsDelete(1);
        iBasketTeamCaptainHistoryService.updateById(basketTeamCaptainHistory);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTeamCaptainHistoryDto basketTeamCaptainHistoryDto){
        if (CollectionUtils.isEmpty(basketTeamCaptainHistoryDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTeamCaptainHistory> list = new ArrayList<>();
        basketTeamCaptainHistoryDto.getIdList().stream().forEach(id ->{
        BasketTeamCaptainHistory basketTeamCaptainHistory = iBasketTeamCaptainHistoryService.getById(id);
            if (ObjectUtils.isEmpty(basketTeamCaptainHistory)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTeamCaptainHistory.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTeamCaptainHistory.setIsDelete(1);
            list.add(basketTeamCaptainHistory);
        });
        iBasketTeamCaptainHistoryService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTeamCaptainHistoryDto basketTeamCaptainHistoryDto){
        return iBasketTeamCaptainHistoryService.addOrUpdate(basketTeamCaptainHistoryDto);
    }

}
