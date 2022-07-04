package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "OrderPayTimesGroup",topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC)
public class OrderPayTimeOutListener implements RocketMQListener<String> {

    @Autowired
    private IOrderInfoService orderInfoService;

    //延迟消息对超时订单处理
    @Override
    public void onMessage(String orderNo) {
        //对超时订单修改
        orderInfoService.cancelTimeOutOrder(orderNo);
    }
}
