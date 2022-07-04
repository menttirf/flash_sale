package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "orderPeddingGroup", topic = MQConstant.ORDER_PEDDING_TOPIC)
public class OrderPeddingListener implements RocketMQListener<OrderMessage> {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private IOrderInfoService orderInfoService;

    @Override
    public void onMessage(OrderMessage message) {
        System.out.println("进行异步下单逻辑");
        OrderMQResult result = new OrderMQResult();
        result.setToken(message.getToken());
        String tag = ":";
        //消费消息
        try {
            //成功下单,orderNo订单号
            String orderNo = orderInfoService.doSeckill(message.getUserPhone(), message.getSeckillId(), message.getTime());
            result.setOrderNo(orderNo);
            //标明订单成功
            tag += MQConstant.ORDER_RESULT_SUCCESS_TAG;

            //发送延时消息
            Message<String> orderNoMsg = MessageBuilder.withPayload(orderNo).build();
            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC,orderNoMsg,3000,MQConstant.ORDER_PAY_TIMEOUT_DELAY_LEVEL);
        } catch (Exception e) {
            e.printStackTrace();
            //下单失败
            result.setTime(message.getTime());
            result.setSeckillId(message.getSeckillId());
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            //标明订单失败
            tag += MQConstant.ORDER_RESULT_FAIL_TAG;
        }
        //将结果消息发送给消息队列
        rocketMQTemplate.syncSend(MQConstant.ORDER_RESULT_TOPIC + tag, result);
    }
}
