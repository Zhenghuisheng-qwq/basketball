package com.zhs.controller;



import com.zhs.service.IBasketMatchTeamStatService;
import com.zhs.model.BasketMatchTeamStat;
import com.zhs.dto.BasketMatchTeamStatDto;

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
@RequestMapping("/basket/match/team/stat")
@Api(value = "")
@Slf4j
public class BasketMatchTeamStatController {

    @Resource
    private IBasketMatchTeamStatService  iBasketMatchTeamStatService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMatchTeamStat>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMatchTeamStat> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatchTeamStat> lambda = new QueryWrapper<BasketMatchTeamStat>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMatchTeamStat::getIsDelete,0);
        IPage<BasketMatchTeamStat> pages =  iBasketMatchTeamStatService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMatchTeamStat> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMatchTeamStat> wrapper = new QueryWrapper<BasketMatchTeamStat>()
                            .lambda().eq(BasketMatchTeamStat::getId,id).eq(BasketMatchTeamStat::getIsDelete,0);
        return R.ok(iBasketMatchTeamStatService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMatchTeamStat> query = new QueryWrapper<BasketMatchTeamStat>().lambda().eq(BasketMatchTeamStat::getId, id).eq(BasketMatchTeamStat::getIsDelete, 0);
        BasketMatchTeamStat basketMatchTeamStat = iBasketMatchTeamStatService.getOne(query);
        if(ObjectUtils.isEmpty(basketMatchTeamStat)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMatchTeamStat.setIsDelete(1);
        iBasketMatchTeamStatService.updateById(basketMatchTeamStat);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMatchTeamStatDto basketMatchTeamStatDto){
        if (CollectionUtils.isEmpty(basketMatchTeamStatDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMatchTeamStat> list = new ArrayList<>();
        basketMatchTeamStatDto.getIdList().stream().forEach(id ->{
        BasketMatchTeamStat basketMatchTeamStat = iBasketMatchTeamStatService.getById(id);
            if (ObjectUtils.isEmpty(basketMatchTeamStat)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMatchTeamStat.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMatchTeamStat.setIsDelete(1);
            list.add(basketMatchTeamStat);
        });
        iBasketMatchTeamStatService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMatchTeamStatDto basketMatchTeamStatDto){
        return iBasketMatchTeamStatService.addOrUpdate(basketMatchTeamStatDto);
    }

}
