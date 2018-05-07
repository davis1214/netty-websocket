package com.even.websocket.handler;

import com.corundumstudio.socketio.SocketIOServer;
import com.even.websocket.annation.Log;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ServerRunner implements CommandLineRunner {


    @Log
    private Logger log;

    private SocketIOServer server;

    @Autowired
    public ServerRunner(SocketIOServer server) {
        this.server = server;
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("------- start socket io server --------");

        server.start();

        log.info("------- socket io server started! --------");

    }
}
