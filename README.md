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


### 描述
1. 增加支持一对一的聊天功能
2. 增加基于user的消息排除功能（join、leave、send等方法）
3. 分布式环境下，增加user/client的消息排除
4. 调整disconnect，先处理业务程序，再处理关闭链接
5. 分布式环境下，增加dispatch后的消息回调功能
