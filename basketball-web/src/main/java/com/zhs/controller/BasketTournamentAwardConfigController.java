package com.zhs.controller;



import com.zhs.service.IBasketTournamentAwardConfigService;
import com.zhs.model.BasketTournamentAwardConfig;
import com.zhs.dto.BasketTournamentAwardConfigDto;

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
@RequestMapping("/basket/tournament/award/config")
@Api(value = "")
@Slf4j
public class BasketTournamentAwardConfigController {

    @Resource
    private IBasketTournamentAwardConfigService  iBasketTournamentAwardConfigService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTournamentAwardConfig>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTournamentAwardConfig> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournamentAwardConfig> lambda = new QueryWrapper<BasketTournamentAwardConfig>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTournamentAwardConfig::getIsDelete,0);
        IPage<BasketTournamentAwardConfig> pages =  iBasketTournamentAwardConfigService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTournamentAwardConfig> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTournamentAwardConfig> wrapper = new QueryWrapper<BasketTournamentAwardConfig>()
                            .lambda().eq(BasketTournamentAwardConfig::getId,id).eq(BasketTournamentAwardConfig::getIsDelete,0);
        return R.ok(iBasketTournamentAwardConfigService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTournamentAwardConfig> query = new QueryWrapper<BasketTournamentAwardConfig>().lambda().eq(BasketTournamentAwardConfig::getId, id).eq(BasketTournamentAwardConfig::getIsDelete, 0);
        BasketTournamentAwardConfig basketTournamentAwardConfig = iBasketTournamentAwardConfigService.getOne(query);
        if(ObjectUtils.isEmpty(basketTournamentAwardConfig)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTournamentAwardConfig.setIsDelete(1);
        iBasketTournamentAwardConfigService.updateById(basketTournamentAwardConfig);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTournamentAwardConfigDto basketTournamentAwardConfigDto){
        if (CollectionUtils.isEmpty(basketTournamentAwardConfigDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTournamentAwardConfig> list = new ArrayList<>();
        basketTournamentAwardConfigDto.getIdList().stream().forEach(id ->{
        BasketTournamentAwardConfig basketTournamentAwardConfig = iBasketTournamentAwardConfigService.getById(id);
            if (ObjectUtils.isEmpty(basketTournamentAwardConfig)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTournamentAwardConfig.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTournamentAwardConfig.setIsDelete(1);
            list.add(basketTournamentAwardConfig);
        });
        iBasketTournamentAwardConfigService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTournamentAwardConfigDto basketTournamentAwardConfigDto){
        return iBasketTournamentAwardConfigService.addOrUpdate(basketTournamentAwardConfigDto);
    }

}
