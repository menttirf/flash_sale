package cn.wolfcode.service;


import cn.wolfcode.domain.OrderInfo;

import java.util.Map;

/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    /**
     * 查询账单
     * @param orderNo
     * @return
     */
    OrderInfo queryByFind(String orderNo);

    /**
     * 生成账单
     * @param phone
     * @param seckillId
     * @param time
     * @return
     */
    String doSeckill(String phone, Long seckillId, Integer time);

    /**
     * 回补redis库存数
     * @param orderNo
     */
    void orderMQResultCount(String orderNo);

    /**
     * 对超时订单处理
     * @param orderNo
     */
    void cancelTimeOutOrder(String orderNo);

    /**
     * html，在线支付
     * @param orderNo
     * @return
     */
    String payOnLine(String orderNo);

    /**
     * 订单状态修改
     * @param orderNo
     */
    void paySuccess(String orderNo);

    /**
     * 支付宝退款
     * @param orderNo
     */
    void refundOnLine(String orderNo);

    /**
     * 积分支付
     * @param orderNo
     * @return
     */
    void integralPay(String orderNo);
}
