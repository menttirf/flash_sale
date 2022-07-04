package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.feign.ProductFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by lanxw
 */
@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ProductFeignApi productFeignApi;

    //mysql
    @Override
    public List<SeckillProductVo> queryByTime(Integer time) {
        List<SeckillProductVo> seckillProductVos = new ArrayList<>();
        //查询秒杀商品消息
        List<SeckillProduct> seckillProducts = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if (seckillProducts.size() == 0) {
            return seckillProductVos;
        }
        //获取秒杀商品的id信息
        List<Long> seckillProductIds = new ArrayList<>();
        for (SeckillProduct seckillProduct : seckillProducts) {
            seckillProductIds.add(seckillProduct.getProductId());
        }
        //调用product远程信息
        Result<List<Product>> result = productFeignApi.queryByIds(seckillProductIds);
        if (result == null || result.hasError()){
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        //根据获取到的秒杀商品id为条件对商品表远程调用
        List<Product> productList = result.getData();
        //将查询出来的商品信息进行map封装
        //目的：根据商品的id，获取对应的对象
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : productList) {
            productMap.put(product.getId(),product);
        }
        //将商品信息进行聚合，使用商品vo进行封装
        for (SeckillProduct seckillProduct : seckillProducts) {
            //更具秒杀商品id获取商品对象
            Product product = productMap.get(seckillProduct.getProductId());
            SeckillProductVo vo = new SeckillProductVo();
            //商品对象封装vo
            BeanUtils.copyProperties(product,vo);
            //秒杀商品对象封装vo
            BeanUtils.copyProperties(seckillProduct,vo);
            //vo集合
            seckillProductVos.add(vo);
        }
        return seckillProductVos;
    }
    //mysql
    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        //根据商品id查询商品信息
        SeckillProduct seckillProduct = seckillProductMapper.getSeckillProductBySeckillId(seckillId);
        if (seckillProduct == null) {
            throw new BusinessException(CommonCodeMsg.ILLEGAL_OPERATION);
        }
        //获取秒杀商品的id信息
        List<Long> seckillProductIds = new ArrayList<>();
        seckillProductIds.add(seckillProduct.getProductId());
        //调用product远程信息
        Result<List<Product>> result = productFeignApi.queryByIds(seckillProductIds);
        if (result == null || result.hasError()){
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        //根据获取到的秒杀商品id为条件对商品表远程调用
        Product product = result.getData().get(0);
        SeckillProductVo vo = new SeckillProductVo();
        //商品对象封装vo
        BeanUtils.copyProperties(product,vo);
        //秒杀商品对象封装vo
        BeanUtils.copyProperties(seckillProduct,vo);
        return vo;
    }

    @Override
    public Integer queryByStockCount(Integer time,Long seckillId) {
        return seckillProductMapper.queryByStockCount(time,seckillId);
    }

    //================================================================
    //使用redis获取数据信息
    @Override
    public List<SeckillProductVo> queryByRedisTime(Integer time) {
        List<SeckillProductVo> vo = new ArrayList<>();
        String seckillKey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        List<Object> values = redisTemplate.opsForHash().values(seckillKey);
        for (Object value : values) {
            String strObj = (String) value;
            vo.add(JSON.parseObject(strObj, SeckillProductVo.class));
        }
        return vo;
    }

    @Override
    public SeckillProductVo redisByfind(Integer time, Long seckillId) {
        String seckillKey = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        Object obj = redisTemplate.opsForHash().get(seckillKey, String.valueOf(seckillId));
        String strObj = (String) obj;
        return JSON.parseObject(strObj,SeckillProductVo.class);
    }

    @Override
    public int decrStock(Long seckillId) {
        return seckillProductMapper.decrStock(seckillId);
    }

    @Override
    public void incrStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }

    @Override
    public void syncStockCountToRedis(Integer time, Long seckillId) {
        //获取数据库库存数
        Integer stockCount = seckillProductMapper.queryByStockCount(time, seckillId);
        if (stockCount != 0) {
            String realKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            redisTemplate.opsForHash().put(realKey,String.valueOf(seckillId),String.valueOf(stockCount));
        }
    }


}
