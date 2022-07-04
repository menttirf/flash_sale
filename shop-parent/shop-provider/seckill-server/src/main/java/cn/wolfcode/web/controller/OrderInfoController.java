package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.DateUtil;
import cn.wolfcode.util.UserUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    @RequireLogin
    @RequestMapping("/doSeckill")
    public Result<String> doSeckill(Integer time, Long seckillId, HttpServletRequest request) {
        //0.判断用户是否登录 --- > 贴 @RequireLogin 直接
        //1.判断时间的合法性
        boolean legalTime = DateUtil.isLegalTime(new Date(), time);
        /*if (!legalTime) {//false
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }*/
        //2.获取请求头token信息,根据token得到用户账号（号码）
        String token = request.getHeader(CommonConstants.TOKEN_NAME);
        String phone = UserUtil.getUserPhone(redisTemplate, token);
        //3.判断库存数量是否充足--mysql
        //Integer stockCount = seckillProductService.queryByStockCount(time,seckillId);
        //优化，使用redis判断库存数
        String seckillCountKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
        //判断商品是否存在
        //优化，扣除redis数据库库存
        Long stockCount = redisTemplate.opsForHash().increment(seckillCountKey, String.valueOf(seckillId), -1);
        if (stockCount < 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        //判断用户是否已经下单
        String setRealKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillId));
        if (redisTemplate.opsForSet().isMember(setRealKey, phone)) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //异步下单 todo 发送同步消息
        OrderMessage orderMessage = new OrderMessage(time,seckillId,token,phone);
        rocketMQTemplate.syncSend(MQConstant.ORDER_PEDDING_TOPIC,orderMessage);

        return Result.success("商品下单成功!");
    }

    @RequestMapping("/find")
    public Result<OrderInfo> find(String orderNo) {
        OrderInfo orderInfo = orderInfoService.queryByFind(orderNo);
        return Result.success(orderInfo);
    }
}
