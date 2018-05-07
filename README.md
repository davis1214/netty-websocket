# netty-websocket
基于netty-com.corundumstudio.socketio 的spring boot方式建立websocket链接，支持消息集群发送，消息集群处理

## 调整
### feature
- ack of dispatch
- filter default room
- add excluded clients in dispatch mode
- add uid map to room mode

### bug-fix
- status error in disconnect event

