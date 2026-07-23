package com.zhs.controller;



import com.zhs.service.IBasketMatchPeriodScoreService;
import com.zhs.model.BasketMatchPeriodScore;
import com.zhs.dto.BasketMatchPeriodScoreDto;

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
@RequestMapping("/basket/match/period/score")
@Api(value = "")
@Slf4j
public class BasketMatchPeriodScoreController {

    @Resource
    private IBasketMatchPeriodScoreService  iBasketMatchPeriodScoreService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketMatchPeriodScore>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketMatchPeriodScore> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketMatchPeriodScore> lambda = new QueryWrapper<BasketMatchPeriodScore>().lambda();
        //此处可以拼条件
        lambda.eq(BasketMatchPeriodScore::getIsDelete,0);
        IPage<BasketMatchPeriodScore> pages =  iBasketMatchPeriodScoreService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketMatchPeriodScore> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketMatchPeriodScore> wrapper = new QueryWrapper<BasketMatchPeriodScore>()
                            .lambda().eq(BasketMatchPeriodScore::getId,id).eq(BasketMatchPeriodScore::getIsDelete,0);
        return R.ok(iBasketMatchPeriodScoreService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketMatchPeriodScore> query = new QueryWrapper<BasketMatchPeriodScore>().lambda().eq(BasketMatchPeriodScore::getId, id).eq(BasketMatchPeriodScore::getIsDelete, 0);
        BasketMatchPeriodScore basketMatchPeriodScore = iBasketMatchPeriodScoreService.getOne(query);
        if(ObjectUtils.isEmpty(basketMatchPeriodScore)) throw new ServiceException("该数据不存在或者已经被删除");
        basketMatchPeriodScore.setIsDelete(1);
        iBasketMatchPeriodScoreService.updateById(basketMatchPeriodScore);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketMatchPeriodScoreDto basketMatchPeriodScoreDto){
        if (CollectionUtils.isEmpty(basketMatchPeriodScoreDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketMatchPeriodScore> list = new ArrayList<>();
        basketMatchPeriodScoreDto.getIdList().stream().forEach(id ->{
        BasketMatchPeriodScore basketMatchPeriodScore = iBasketMatchPeriodScoreService.getById(id);
            if (ObjectUtils.isEmpty(basketMatchPeriodScore)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketMatchPeriodScore.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketMatchPeriodScore.setIsDelete(1);
            list.add(basketMatchPeriodScore);
        });
        iBasketMatchPeriodScoreService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketMatchPeriodScoreDto basketMatchPeriodScoreDto){
        return iBasketMatchPeriodScoreService.addOrUpdate(basketMatchPeriodScoreDto);
    }

}
