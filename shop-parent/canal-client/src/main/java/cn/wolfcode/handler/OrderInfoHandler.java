package cn.wolfcode.handler;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.redis.SeckillRedisKey;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

@Component
@CanalTable(value = "t_order_info")
public class OrderInfoHandler implements EntryHandler<OrderInfo> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    //监听插入的数据
    @Override
    public void insert(OrderInfo orderInfo) {
        //创建账单--redis
        String realKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(orderInfo.getSeckillId()));
        redisTemplate.opsForSet().add(realKey, String.valueOf(orderInfo.getUserId()));

        //将创建的账单存到redis中
        String orderNoKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        redisTemplate.opsForHash().put(orderNoKey,orderInfo.getOrderNo(), JSON.toJSONString(orderInfo));
    }

    //监听修改的数据
    @Override
    public void update(OrderInfo before, OrderInfo after) {
        String orderNoKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        redisTemplate.opsForHash().put(orderNoKey,after.getOrderNo(), JSON.toJSONString(after));
    }
}
