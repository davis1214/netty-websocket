/**
 * Copyright 2012 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.misc.IterableCollection;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.DispatchMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fully thread-safe.
 */
public class BroadcastOperations implements ClientOperations {

    private final Iterable<SocketIOClient> clients;
    private final StoreFactory storeFactory;

    private String room;

    public BroadcastOperations(Iterable<SocketIOClient> clients, StoreFactory storeFactory) {
        super();
        this.clients = clients;
        this.storeFactory = storeFactory;
    }

    public BroadcastOperations(Iterable<SocketIOClient> clients, StoreFactory storeFactory, String room) {
        super();
        this.clients = clients;
        this.storeFactory = storeFactory;
        this.room = room;
    }

    private void roomId(String room) {
        this.room = room;
    }

    private void dispatch(Packet packet) {
        //完全基于room来进行分发
        Map<String, Set<String>> namespaceRooms = new HashMap<String, Set<String>>();
        for (SocketIOClient socketIOClient : clients) {
            Namespace namespace = (Namespace) socketIOClient.getNamespace();
            Set<String> rooms = namespace.getRooms(socketIOClient);

            Set<String> roomsList = namespaceRooms.get(namespace.getName());
            if (roomsList == null) {
                roomsList = new HashSet<String>();
                namespaceRooms.put(namespace.getName(), roomsList);
            }
            roomsList.addAll(rooms);
        }

        for (Entry<String, Set<String>> entry : namespaceRooms.entrySet()) {
            for (String room : entry.getValue()) {

                if ("".equals(room)) {
                    System.out.println("packet = [" + packet + "] , room is null .would not dispatch !");
                    continue;
                }

                storeFactory.pubSubStore().publish(PubSubType.DISPATCH, new DispatchMessage(room, packet, entry.getKey()));
            }
        }
    }

    public Collection<SocketIOClient> getClients() {
        return new IterableCollection<SocketIOClient>(clients);
    }

    @Override
    public void send(Packet packet) {
        List<String> excludeClients = Lists.newArrayList();
        if (packet.getExcludeClients() != null) {
            excludeClients = Arrays.asList(packet.getExcludeClients().split(","));
        }

        System.out.println("packet = [" + packet + "]");
        AtomicBoolean shouldDispatch = new AtomicBoolean(true);
        for (SocketIOClient client : clients) {
            System.out.println("excludeClients = [" + excludeClients + "] , cur clientId " + client.getSessionId().toString());

            if (excludeClients.contains(client.getSessionId().toString())) {
                continue;
            }

            client.send(packet);
            shouldDispatch.set(false);
        }

        if (shouldDispatch.get()) {
            dispatch(packet);
        }
    }

    public <T> void send(Packet packet, BroadcastAckCallback<T> ackCallback) {
        for (SocketIOClient client : clients) {
            client.send(packet, ackCallback.createClientCallback(client));
        }
        ackCallback.loopFinished();
    }

    @Override
    public void disconnect() {
        for (SocketIOClient client : clients) {
            client.disconnect();
        }
    }

    public void sendEvent(String name, SocketIOClient excludedClient, Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setExcludeClients(excludedClient.getSessionId().toString());
        packet.setData(Arrays.asList(data));

        AtomicBoolean shouldDispatch = new AtomicBoolean(true);
        for (SocketIOClient client : clients) {

            if (client.getSessionId().equals(excludedClient.getSessionId())) {
                continue;
            }

            //不发送当前聊天时
            AtomicBoolean shouldSkip = new AtomicBoolean(true);
            client.getAllRooms().stream().forEach(room -> {
                if (room.equals(this.room)) {
                    shouldSkip.set(false);
                }
            });

            if (shouldSkip.get()) {
                continue;
            }

            shouldDispatch.set(false);
            client.send(packet);
        }

        if (shouldDispatch.get()) {
            dispatch(packet);
        }
    }

    @Override
    public void sendEvent(String name, Object... data) {
        sendEventWithExcludeClients(name, "", data);
    }

    @Override
    public void sendEventWithExcludeClients(String name, String excludeClients, Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setExcludeClients(excludeClients);
        packet.setData(Arrays.asList(data));
        send(packet);
    }

    @Override
    public void sendEventWithExcludeUserKeys(String name, String excludeUserKeys, Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setExcludeUserKeys(excludeUserKeys);
        packet.setData(Arrays.asList(data));
        send(packet);
    }

    public <T> void sendEvent(String name, Object data, BroadcastAckCallback<T> ackCallback) {
        for (SocketIOClient client : clients) {
            client.sendEvent(name, ackCallback.createClientCallback(client), data);
        }
        ackCallback.loopFinished();
    }

    public <T> void sendEvent(String name, Object data, SocketIOClient excludedClient, BroadcastAckCallback<T> ackCallback) {
        AtomicBoolean shouldDispatch = new AtomicBoolean(true);

        for (SocketIOClient client : clients) {
            if (client.getSessionId().equals(excludedClient.getSessionId())) {
                continue;
            }

            AtomicBoolean shouldSkip = new AtomicBoolean(true);
            client.getAllRooms().stream().forEach(room -> {
                if (room.equals(this.room)) {
                    shouldSkip.set(false);
                }
            });

            if (shouldSkip.get()) {
                continue;
            }
            shouldDispatch.set(false);
            client.sendEvent(name, ackCallback.createClientCallback(client), data);
        }

        if (shouldDispatch.get()) {
            Packet packet = new Packet(PacketType.MESSAGE);
            packet.setSubType(PacketType.EVENT);
            packet.setName(name);
            packet.setExcludeClients(excludedClient.getSessionId().toString());
            packet.setData(Arrays.asList(data));

            System.out.println("name = [" + name + "], should dispatch , excludedClient = [" + excludedClient.getSessionId() + "]");

            try {
                dispatch(packet);
            } catch (Exception e) {
                e.printStackTrace();
                ackCallback.onClientTimeout(null);
            }
        }

        ackCallback.loopFinished();
    }


}
