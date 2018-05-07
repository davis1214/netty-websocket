package com.even.websocket.handler;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.even.websocket.annation.Log;
import com.even.websocket.msg.MessageInfo;
import com.even.websocket.msg.StatusVo;
import jodd.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageEventHandler {

    @Log
    private Logger log;

    @Autowired
    private SocketIOServer server;

    private final String uerKey = "";


    private ConcurrentHashMap<String, String> SessionIdToClientId = new ConcurrentHashMap();

    @OnConnect
    public void onConnect(SocketIOClient client) {
        final String roomId = client.getHandshakeData().getSingleUrlParam("roomId");
        if (StringUtils.isEmpty(roomId)) {

            log.error("on connect ,empty request ,client id {}, address {}", client.getSessionId(), client.getRemoteAddress());
            return;
        }
        SessionIdToClientId.put(client.getSessionId().toString(), roomId);

        //final SocketIONamespace socketIONamespace = server.addNamespace(roomId);

        //client.joinRoom(roomId);

        //TODO add userKey to clientID

        client.joinRoom(roomId, uerKey);

//        server.addNamespace(roomId).getClient(client.getSessionId()).joinRoom(roomId);
        log.info("client connect to the server ,sessionId={} , roomId {}", client.getSessionId().toString(), roomId);
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {

        final String roomId = SessionIdToClientId.get(client.getSessionId().toString());
        if (StringUtil.isEmpty(roomId)) {
            log.error("on connect ,empty request ,client id {}, address {}", client.getSessionId(), client.getRemoteAddress());
            return;
        }

        SessionIdToClientId.remove(client.getSessionId().toString());

//        final SocketIONamespace namespace = server.getNamespace(roomId);
//        if (namespace == null) {
//            log.error("onDisconnect , namespace is null!");
//            return;
//        }

        //client.leaveRoom(roomId);

        //TODO
        client.leaveRoom(roomId, uerKey);


        server.removeNamespace(roomId);
        log.info("client disconnect to the server ,sessionId={} , room id {}", client.getSessionId().toString(), roomId);

//        client.leaveRoom(client.getHandshakeData().getSingleUrlParam("roomId"));
    }

    //消息接收入口，当接收到消息后，查找发送目标客户端
    @OnEvent(value = "teacher")
    public void onTeacherEvnet(SocketIOClient client, AckRequest request, MessageInfo data) {
        Objects.requireNonNull(data);

        server.getAllClients().stream().forEach(cc -> {
            log.info("onTeacherEvnet - client info in server :" + cc.getRemoteAddress() + "" + " ," + cc.getSessionId());
        });
        log.info("onTeacherEvnet -> client address {} , sessionid {}, on event {} , message {}", client.getRemoteAddress(), client.getSessionId().toString(), "teacher", data);

//        final SocketIONamespace namespace = server.getNamespace(data.getRoomId());
//        if (namespace == null) {
//            log.error("teacher , namespace is null!");
//            return;
//        }

        final BroadcastOperations roomOperations = server.getRoomOperations(data.getRoomId());

        roomOperations.getClients();

        if (roomOperations == null) {
            log.error("teacher , roomOperations is null!");
            return;
        }

        //roomOperations.sendEvent(data.getEventType(), data);

        roomOperations.sendEvent(data.getEventType(), client, data);

        // server.getRoomOperations(data.getRoomId()).sendEvent(data.getEventType(), data);

//        server.getBroadcastOperations().
    }

    @OnEvent(value = "student")
    public void onStudentEvent(SocketIOClient client, AckRequest request, MessageInfo data) {
        Objects.requireNonNull(data);

        server.getAllClients().stream().forEach(cc -> {
            log.info("onStudentEvent - client info in server :" + cc.getRemoteAddress() + "  , " + cc.getSessionId());
        });

        log.info("onStudentEvent -> client address {} , sessionid {},on event {} , message {}", client.getRemoteAddress(), client.getSessionId().toString(), "student", data);

        //final BroadcastOperations roomOperations = server.getRoomOperations(data.getRoomId());

        String excludeUserKeys = "";
        final BroadcastOperations roomOperations = server.getRoomOperations(data.getRoomId(), excludeUserKeys);

        if (roomOperations == null) {
            log.error("student , roomOperations is null!");
            return;
        }
        //roomOperations.sendEvent(data.getEventType(), client, data);

        StatusVo statusVo = new StatusVo();
        BroadcastAckCallback ackCallback = new BroadcastAckCallback(statusVo.getClass());
        roomOperations.sendEvent(data.getEventType(), data, client, ackCallback);

        log.info("statusVo {} , ack request {} ", statusVo);
    }

    @OnEvent(value = "acks")
    public void onAckEvent(SocketIOClient client, AckRequest request, MessageInfo data) {
        Objects.requireNonNull(data);

        log.info("onStudentEvent -> client address {} , sessionid {},on event {} , message {}", client.getRemoteAddress(), client.getSessionId().toString(), "student", data);

        server.getAllClients().stream().forEach(cc -> {
            log.info("onStudentEvent - client info in server :" + cc.getRemoteAddress() + "  , " + cc.getSessionId());
        });


        final BroadcastOperations roomOperations = server.getRoomOperations(data.getRoomId());

        if (roomOperations == null) {
            log.error("student , roomOperations is null!");
            return;
        }


        //String name, Object data, SocketIOClient excludedClient, BroadcastAckCallback<T> ackCallback

        StatusVo statusVo = new StatusVo();
        BroadcastAckCallback ackCallback = new BroadcastAckCallback(statusVo.getClass());
        roomOperations.sendEvent(data.getEventType(), data, client, ackCallback);


        //roomOperations.sendEvent(data.getEventType(), client, data);
    }

}
