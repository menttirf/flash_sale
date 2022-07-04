package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;

/**
 * Created by lanxw
 */
public interface ISeckillProductService {
    /**
     * 查询秒杀商品页面信息
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTime(Integer time);

    /**
     * 查询秒杀商品的明细
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo find(Integer time, Long seckillId);

    /**
     * 获取库存数
     * @param seckillId
     * @return
     */
    Integer queryByStockCount(Integer time,Long seckillId);

    /**
     * 使用redis进行数据查询
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByRedisTime(Integer time);

    /**
     * 使用redis进行明细查询
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo redisByfind(Integer time, Long seckillId);

    /**
     * 添加账单
     * @param seckillId
     * @return
     */
    int decrStock(Long seckillId);

    /**
     * 回补库存数
     * @param seckillTime
     * @param seckillId
     */
    void syncStockCountToRedis(Integer seckillTime, Long seckillId);

    /**
     * 增加数据库库存
     * @param seckillId
     */
    void incrStockCount(Long seckillId);

}
