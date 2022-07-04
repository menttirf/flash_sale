package cn.wolfcode.mq;

import cn.wolfcode.ws.OrderResultWebSocketServer;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RocketMQMessageListener(consumerGroup = "OrderResultGroup"
        ,topic = MQConstants.ORDER_RESULT_TOPIC)
public class OrderResultListener  implements RocketMQListener<OrderMQResult> {
    @Override
    public void onMessage(OrderMQResult message) {
        System.out.println("消息通知：" + JSON.toJSONString(message));

        String token = message.getToken();
        int count = 0;
        do {
            Session session = OrderResultWebSocketServer.clients.get(token);
            if (session != null) {
                try {
                    //将消息的结果以文本消息的形式发给websocket,getBasicRemote同步方法
                    session.getBasicRemote().sendText(JSON.toJSONString(message));
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }while (count < 3);
    }
}
