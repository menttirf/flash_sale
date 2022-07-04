package cn.wolfcode.job;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.redis.JobRedisKey;
import cn.wolfcode.web.feign.SeckillProductVoApi;
import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Setter
@Getter
public class SeckillProductCacheJob implements SimpleJob {
    @Value("${jobCron.initSeckillProduct}")
    private String cron;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SeckillProductVoApi seckillProductVoApi;

    @Override
    public void execute(ShardingContext shardingContext) {
        //定时任务，将商品服务从MySQL上传到redis
        //获取到任务项参数（分片），time：0=10，1=12，2=14
        String time = shardingContext.getShardingParameter();
        doWord(time);
    }

    private void doWord(String time) {
        //远程调度product商品服务表,获取商品信息
        Result<List<SeckillProductVo>> result = seckillProductVoApi.seckillProductVoList(Integer.parseInt(time));
        if (result == null || result.hasError()){
            return;
        }
        //获取商品消息
        List<SeckillProductVo> seckillProductVos = result.getData();
        //初始化商品信息
        String seckillKey = JobRedisKey.SECKILL_PRODUCT_HASH.getRealKey(time);
        //初始化库存
        String seckillCountKey = JobRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(time);
        //把前一天相关信息进行删除
        redisTemplate.delete(seckillKey);
        //使用hash数据类型进行对vo对象的封装
        for (SeckillProductVo vo : seckillProductVos) {
            //初始化商品信息，以不同时间段的商品的参数为key
            redisTemplate.opsForHash().put(seckillKey, String.valueOf(vo.getId()), JSON.toJSONString(vo));
            //初始化库存，将商品的id为key,库存数为value
            redisTemplate.opsForHash().put(seckillCountKey,String.valueOf(vo.getId()),String.valueOf(vo.getStockCount()));
        }
        System.out.println("同步完成!!!!");
    }
}
