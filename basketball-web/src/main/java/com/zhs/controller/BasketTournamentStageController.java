package com.zhs.controller;



import com.zhs.service.IBasketTournamentStageService;
import com.zhs.model.BasketTournamentStage;
import com.zhs.dto.BasketTournamentStageDto;

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
@RequestMapping("/basket/tournament/stage")
@Api(value = "")
@Slf4j
public class BasketTournamentStageController {

    @Resource
    private IBasketTournamentStageService  iBasketTournamentStageService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentStage>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentStage> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentStage> lambda = new QueryWrapper<BasketTournamentStage>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentStage::getIsDelete,0);
        IPage<BasketTournamentStage> pages =  iBasketTournamentStageService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentStage> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentStage> wrapper = new QueryWrapper<BasketTournamentStage>()
                            .lambda().eq(BasketTournamentStage::getId,id).eq(BasketTournamentStage::getIsDelete,0);
        return R.ok(iBasketTournamentStageService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentStage> query = new QueryWrapper<BasketTournamentStage>().lambda().eq(BasketTournamentStage::getId, id).eq(BasketTournamentStage::getIsDelete, 0);
        BasketTournamentStage basketTournamentStage = iBasketTournamentStageService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentStage)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentStage.setIsDelete(1);
        iBasketTournamentStageService.updateById(basketTournamentStage);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentStageDto basketTournamentStageDto){
        if (CollectionUtils.isEmpty(basketTournamentStageDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentStage> list = new ArrayList<>();
        basketTournamentStageDto.getIdList().stream().forEach(id ->{
        BasketTournamentStage basketTournamentStage = iBasketTournamentStageService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentStage)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentStage.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentStage.setIsDelete(1);
            list.add(basketTournamentStage);
        });
        iBasketTournamentStageService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentStageDto basketTournamentStageDto){
        return iBasketTournamentStageService.addOrUpdate(basketTournamentStageDto);
    }

}
