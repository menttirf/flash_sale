package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "OrderMQResultGroup",
        topic = MQConstant.ORDER_RESULT_TOPIC,
        selectorExpression = MQConstant.ORDER_RESULT_FAIL_TAG)
public class OrderMQResultListener implements RocketMQListener<OrderMQResult> {

    @Autowired
    private IOrderInfoService orderInfoService;

    //监听消息的结果，如果为false，执行回补
    @Override
    public void onMessage(OrderMQResult message) {
        orderInfoService.orderMQResultCount(message.getOrderNo());
    }
}
