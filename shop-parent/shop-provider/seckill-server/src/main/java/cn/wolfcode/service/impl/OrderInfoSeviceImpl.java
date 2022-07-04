package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.IRefundLogService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.feign.IntegralFeignApi;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by wolfcode-lanxw
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private PayFeignApi payFeignApi;
    @Autowired
    private IRefundLogService refundLogService;
    @Autowired
    private IntegralFeignApi integralFeignApi;

    private String saveBill(String phone, Long seckillId, Integer time) {
        SeckillProductVo vo = seckillProductService.find(time, seckillId);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(String.valueOf(IdGenerateUtil.get().nextId()));
        orderInfo.setUserId(Long.parseLong(phone));
        orderInfo.setProductId(seckillId);
        orderInfo.setProductName(vo.getProductName());
        orderInfo.setProductImg(vo.getProductImg());
        orderInfo.setProductPrice(vo.getProductPrice());
        orderInfo.setSeckillPrice(vo.getSeckillPrice());
        orderInfo.setIntegral(vo.getIntegral());
        orderInfo.setCreateDate(new Date());
        orderInfo.setSeckillDate(vo.getStartDate());
        orderInfo.setSeckillTime(vo.getTime());
        orderInfo.setSeckillId(seckillId);
        orderInfoMapper.insert(orderInfo);

        //返回订单号
        return orderInfo.getOrderNo();
    }

    @Override
    public OrderInfo queryByFind(String orderNo) {
        //redis查询账单
        String orderNoKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        String strObj = (String) redisTemplate.opsForHash().get(orderNoKey, orderNo);
        return JSON.parseObject(strObj,OrderInfo.class);
    }

    @Override
    @Transactional
    public String doSeckill(String phone, Long seckillId, Integer time) {
        //mysql库存数-1
        int stock = seckillProductService.decrStock(seckillId);
        if (stock == 0) {
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }

        //订单号
        String orderNo = saveBill(phone, seckillId, time);

        //创建账单--redis-----优化canal todo
        /*String realKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillId));
        redisTemplate.opsForSet().add(realKey, phone);*/

        return orderNo;
    }

    @Override
    public void orderMQResultCount(String orderNo) {
        //根据账单号查询账单对象
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //同步预存数
        seckillProductService.syncStockCountToRedis(orderInfo.getSeckillTime(), orderInfo.getSeckillId());
    }

    //预库存回补
    @Override
    @Transactional
    public void cancelTimeOutOrder(String orderNo) {
        //根据账单号查询账单对象
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //查询订单状态是否支付
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            //设置账单状态为超时支付
            int result = orderInfoMapper.updateCancelStatus(orderNo, OrderInfo.STATUS_TIMEOUT);
            if (result == 0) {
                return;
            }
            //增加数据库库存---->超时订单的处理
            seckillProductService.incrStockCount(orderInfo.getSeckillId());
            //同步预存数
            seckillProductService.syncStockCountToRedis(orderInfo.getSeckillTime(), orderInfo.getSeckillId());
        }
    }

    @Value("${pay.returnUrl}")
    private String returnUrl; //同步回调
    @Value("${pay.notifyUrl}")
    private String notifyUrl; //异步回调

    @Override
    public String payOnLine(String orderNo) {
        //根据订单id查询订单信息
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        PayVo vo = new PayVo();
        vo.setSubject(orderInfo.getProductName());
        vo.setTotalAmount(orderInfo.getSeckillPrice().toString());
        vo.setOutTradeNo(orderNo);
        vo.setBody(orderInfo.getProductName());

        //异步与同步回调
        vo.setReturnUrl(returnUrl);
        vo.setNotifyUrl(notifyUrl);
        //远程调用
        Result<String> result = payFeignApi.alipay(vo);
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.PAY_SERVER_ERROR);
        }
        return result.getData();
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo) {
        //修改支付状态
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        if (OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())) {
            int effectCount = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_ONLINE);
            if (effectCount == 0) {
                //订单未支付，在回调之前，已经有其他线程处理了账单
                //发送消息，通知客服，走退款逻辑
                //注意不用抛出异常
            }
        } else {
            //订单未支付，在回调之前，已经有其他线程处理了账单
            //发送消息，通知客服，走退款逻辑
        }
    }


    @Override
    //@Transactional
    @GlobalTransactional
    public void refundOnLine(String orderNo) {
        //查询订单信息
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        if (OrderInfo.PAYTYPE_ONLINE.equals(orderInfo.getPayType())) {
            //支付宝退款
            this.aliPayByPay(orderInfo,orderNo);
        }else if (OrderInfo.PAYTYPE_INTEGRAL.equals(orderInfo.getPayType())){
            this.integralByPay(orderInfo);
        }else {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        //插入流水
        this.ByRefundLog(orderNo,orderInfo.getSeckillPrice().longValue(), orderInfo.getPayType());
    }

    //支付宝退款
    private void aliPayByPay(OrderInfo orderInfo,String orderNo){
        //支付宝退款
        RefundVo vo = new RefundVo();
        vo.setOutTradeNo(orderNo);
        vo.setRefundAmount(orderInfo.getSeckillPrice().toString());
        //远程调用--->支付宝退款功能
        Result<Boolean> result = payFeignApi.refund(vo);
        if (result == null || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        orderInfoMapper.changeRefundStatus(orderNo, OrderInfo.STATUS_REFUND);
    }
    //积分退款
    private void integralByPay(OrderInfo orderInfo){
        //int i = 1/0;
        //积分退款
        OperateIntegralVo integralVo = new OperateIntegralVo();
        integralVo.setUserId(orderInfo.getUserId());
        integralVo.setValue(orderInfo.getIntegral());
        //远程调用增加积分
        Result<Boolean> result = integralFeignApi.incrValue(integralVo);
        if (result == null || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        int refundStatus = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(), OrderInfo.STATUS_REFUND);
        if (refundStatus == 0){
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
    }

    //退款记录
    private void ByRefundLog(String orderNo, Long refundAmoun, Integer refundType){
        RefundLog log = new RefundLog();
        log.setOrderNo(orderNo);
        log.setRefundTime(new Date());
        log.setRefundAmount(refundAmoun);
        log.setRefundReason("不需要了");
        log.setRefundType(refundType);
        try {
            refundLogService.saveByRefundLog(log);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //积分支付
    @Override
    @GlobalTransactional
    public void integralPay(String orderNo) {
        //查询订单
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //添加流水账单
        PayLog log = new PayLog();
        log.setOrderNo(orderNo);
        log.setPayTime(new Date());
        log.setPayType(PayLog.PAY_TYPE_INTEGRAL);
        log.setTotalAmount(orderInfo.getSeckillPrice().toString());
        try {
            payLogMapper.insert(log);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        OperateIntegralVo vo = new OperateIntegralVo();
        vo.setUserId(orderInfo.getUserId());
        vo.setValue(orderInfo.getIntegral());
        //远程调用,对用户的积分扣减
        Result<Boolean> result = integralFeignApi.decrValue(vo);
        if (result == null || result.hasError() || !result.getData()) {
            throw new BusinessException(SeckillCodeMsg.INTEGRAL_SERVER_ERROR);
        }
        //修改订单状态---->已支付
        int effectCount = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_INTEGRAL);
        if (effectCount == 0) {
            throw new BusinessException(SeckillCodeMsg.INTEGRAL_SERVER_ERROR);
        }
    }
}
