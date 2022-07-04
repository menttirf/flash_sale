package cn.wolfcode.ws;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@ServerEndpoint("/{token}")
public class OrderResultWebSocketServer {
    public static ConcurrentMap<String, Session> clients = new ConcurrentHashMap<>();

    //浏览器和服务器建立连接的时候会触发此方法
    @OnOpen
    public void onOpen(@PathParam("token") String token,Session session){
        clients.put(token,session);
    }

    //当浏览器给服务器发送消息的时候会触发该方法
    @OnMessage
    public void onMessage(@PathParam("token") String token,String msg){
        System.out.println("客户端:"+token+",发送消息,内容为:"+msg);
    }

    //当浏览器关闭的时候，和服务器断开连接会触发该方法
    @OnClose
    public void onClose(@PathParam("token") String token){
        System.out.println("客户端和服务器断开连接");
        clients.remove(token);
    }
    //当出现异常的时候会触发此方法
    @OnError
    public void onError(Throwable ex){
        System.out.println("通信出现异常");
    }

}
