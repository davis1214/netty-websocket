//package com.even.websocket.handler;
//
//import com.corundumstudio.com.corundumstudio.socketio.AckRequest;
//import com.corundumstudio.com.corundumstudio.socketio.SocketIOClient;
//import com.corundumstudio.com.corundumstudio.socketio.SocketIOServer;
//import com.corundumstudio.com.corundumstudio.socketio.annotation.OnConnect;
//import com.corundumstudio.com.corundumstudio.socketio.annotation.OnDisconnect;
//import com.corundumstudio.com.corundumstudio.socketio.annotation.OnEvent;
//import com.even.websocket.annation.Log;
//import com.even.websocket.msg.MessageInfo;
//import org.slf4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.util.Objects;
//
//@Component
//public class MessageEventHandler1 {
//
//    @Log
//    private Logger log;
//
//    @Autowired
//    private SocketIOServer server;
//
//    @OnConnect
//    public void onConnect(SocketIOClient client) {
//        final String roomId = client.getHandshakeData().getSingleUrlParam("roomId");
//        if (StringUtils.isEmpty(roomId)) {
//
//            log.error("on connect ,empty request ,client id {}, address {}", client.getSessionId(), client.getRemoteAddress());
//            return;
//        }
//        client.joinRoom(roomId);
////        server.addNamespace(roomId).getClient(client.getSessionId()).joinRoom(roomId);
//        log.info("client connect to the server ,sessionId={} , roomId {}", client.getSessionId().toString(), roomId);
//    }
//
//    @OnDisconnect
//    public void onDisconnect(SocketIOClient client) {
//        log.info("client disconnect to the server ,sessionId={}", client.getSessionId().toString());
//
////        client.leaveRoom(client.getHandshakeData().getSingleUrlParam("roomId"));
//    }
//
//    //消息接收入口，当接收到消息后，查找发送目标客户端
//    @OnEvent(value = "teacher")
//    public void onTeacherEvnet(SocketIOClient client, AckRequest request, MessageInfo data) {
//        Objects.requireNonNull(data);
////        String teacherId = client.getHandshakeData().getSingleUrlParam("teacherId");
////        if(StringUtils.isEmpty(teacherId)){
////            return;
////        }
//
//
//        server.getAllClients().stream().forEach(cc -> {
//            log.info("onTeacherEvnet - client info in server :" + cc.getRemoteAddress() + "  , " + cc.getClientIp() + " ," + cc.getSessionId());
//        });
//        log.info("onTeacherEvnet -> client address {} , sessionid {}, on event {} , message {}", client.getRemoteAddress(), client.getSessionId().toString(), "teacher", data);
//
//        server.getRoomOperations(data.getRoomId()).sendEvent(data.getEventType(), data);
//
//
////        server.getBroadcastOperations().
//    }
//
//    @OnEvent(value = "student")
//    public void onStudentEvent(SocketIOClient client, AckRequest request, MessageInfo data) {
//        Objects.requireNonNull(data);
//
//        server.getAllClients().stream().forEach(cc -> {
//            log.info("onStudentEvent - client info in server :" + cc.getRemoteAddress() + "  , " + cc.getClientIp() + " ," + cc.getSessionId());
//        });
//
//        log.info("onStudentEvent -> client address {} , sessionid {},on event {} , message {}", client.getRemoteAddress(), client.getSessionId().toString(), "student", data);
//
//        server.getRoomOperations(data.getRoomId()).sendEvent(data.getEventType(), data);
//
////        server.getNamespace(WebSocketConstant.Namespace).getRoomOperations(data.getRoomId()).sendEvent(data.getEventType(), data);
//    }
//
////    @OnEvent(value = "studentJoinRoom")
////    public void studentConectEvent(SocketIOClient client, AckRequest request, MessageInfo data) {
////        Objects.requireNonNull(data);
////        client.joinRoom(data.getRoomId());
////        log.info("student join the room,roomId={},studentNickName={}", data.getRoomId(), data.getStudentNickName());
////        server.getRoomOperations(data.getRoomId()).sendEvent(data.getEventType(), data);
////    }
////
////    @OnEvent(value = "teacherJoinRoom")
////    public void teacherJoinRoom(SocketIOClient client, AckRequest request, MessageInfo data) {
////        Objects.requireNonNull(data);
////        client.joinRoom(data.getRoomId());
////        log.info("teacher join the room,roomId={},teacherNickName={}", data.getRoomId(), data.getTeacherNickName());
////        server.getRoomOperations(data.getRoomId()).sendEvent(data.getEventType(), data);
////    }
//
//}
