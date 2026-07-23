package com.zhs.controller;



import com.zhs.service.IBasketMatchPlayerStatService;
import com.zhs.model.BasketMatchPlayerStat;
import com.zhs.dto.BasketMatchPlayerStatDto;

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
@RequestMapping("/basket/match/player/stat")
@Api(value = "")
@Slf4j
public class BasketMatchPlayerStatController {

    @Resource
    private IBasketMatchPlayerStatService  iBasketMatchPlayerStatService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMatchPlayerStat>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMatchPlayerStat> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatchPlayerStat> lambda = new QueryWrapper<BasketMatchPlayerStat>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMatchPlayerStat::getIsDelete,0);
        IPage<BasketMatchPlayerStat> pages =  iBasketMatchPlayerStatService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMatchPlayerStat> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMatchPlayerStat> wrapper = new QueryWrapper<BasketMatchPlayerStat>()
                            .lambda().eq(BasketMatchPlayerStat::getId,id).eq(BasketMatchPlayerStat::getIsDelete,0);
        return R.ok(iBasketMatchPlayerStatService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMatchPlayerStat> query = new QueryWrapper<BasketMatchPlayerStat>().lambda().eq(BasketMatchPlayerStat::getId, id).eq(BasketMatchPlayerStat::getIsDelete, 0);
        BasketMatchPlayerStat basketMatchPlayerStat = iBasketMatchPlayerStatService.getOne(query);
        if(ObjectUtils.isEmpty(basketMatchPlayerStat)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMatchPlayerStat.setIsDelete(1);
        iBasketMatchPlayerStatService.updateById(basketMatchPlayerStat);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMatchPlayerStatDto basketMatchPlayerStatDto){
        if (CollectionUtils.isEmpty(basketMatchPlayerStatDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMatchPlayerStat> list = new ArrayList<>();
        basketMatchPlayerStatDto.getIdList().stream().forEach(id ->{
        BasketMatchPlayerStat basketMatchPlayerStat = iBasketMatchPlayerStatService.getById(id);
            if (ObjectUtils.isEmpty(basketMatchPlayerStat)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMatchPlayerStat.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMatchPlayerStat.setIsDelete(1);
            list.add(basketMatchPlayerStat);
        });
        iBasketMatchPlayerStatService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMatchPlayerStatDto basketMatchPlayerStatDto){
        return iBasketMatchPlayerStatService.addOrUpdate(basketMatchPlayerStatDto);
    }

}
