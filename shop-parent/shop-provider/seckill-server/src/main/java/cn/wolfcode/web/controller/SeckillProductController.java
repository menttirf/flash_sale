package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.ISeckillProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by lanxw
 * 秒杀商品信息查询
 */
@RestController
@RequestMapping("/seckillProduct")
@Slf4j
public class SeckillProductController {
    @Autowired
    private ISeckillProductService seckillProductService;

    //秒杀商品页面信息---mysql
    @RequestMapping("/queryByTime")
    public Result<List<SeckillProductVo>> queryByTime(Integer time){
        return Result.success(seckillProductService.queryByRedisTime(time));
    }

    //秒杀商品的明细---mysql
    /*@RequestMapping("/find")
    public Result<SeckillProductVo> find(Integer time,Long seckillId){
        return Result.success(seckillProductService.find(time,seckillId));
    }*/

    //分布式调度----redis
    @RequestMapping("/queryByTimeForApi")
    public Result<List<SeckillProductVo>> seckillProductVoList(Integer time){
        return Result.success(seckillProductService.queryByTime(time));
    }

    //秒杀商品的明细---redis
    @RequestMapping("/find")
    public Result<SeckillProductVo> find(Integer time,Long seckillId){
        return Result.success(seckillProductService.redisByfind(time,seckillId));
    }

}
