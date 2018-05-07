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
package com.corundumstudio.socketio.namespace;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.ScannerEngine;
import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.JoinLeaveMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import com.corundumstudio.socketio.transport.NamespaceClient;
import com.google.common.collect.Lists;
import io.netty.util.internal.PlatformDependent;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hub object for all clients in one namespace.
 * Namespace shares by different namespace-clients.
 *
 * @see com.corundumstudio.socketio.transport.NamespaceClient
 */
public class Namespace implements SocketIONamespace {

    public static final String DEFAULT_NAME = "";

    private final ScannerEngine engine = new ScannerEngine();
    private final ConcurrentMap<String, EventEntry<?>> eventListeners = PlatformDependent.newConcurrentHashMap();
    private final Queue<ConnectListener> connectListeners = new ConcurrentLinkedQueue<ConnectListener>();
    private final Queue<DisconnectListener> disconnectListeners = new ConcurrentLinkedQueue<DisconnectListener>();
    private final Queue<PingListener> pingListeners = new ConcurrentLinkedQueue<PingListener>();

    private final Map<UUID, SocketIOClient> allClients = PlatformDependent.newConcurrentHashMap();
    private final ConcurrentMap<String, Set<UUID>> roomClients = PlatformDependent.newConcurrentHashMap();
    private final ConcurrentMap<UUID, Set<String>> clientRooms = PlatformDependent.newConcurrentHashMap();
    private final ConcurrentMap<String, Set<UUID>> userKeyClients = PlatformDependent.newConcurrentHashMap();

    private final String name;
    private final AckMode ackMode;
    private final JsonSupport jsonSupport;
    private final StoreFactory storeFactory;
    private final ExceptionListener exceptionListener;

    public Namespace(String name, Configuration configuration) {
        super();
        this.name = name;
        this.jsonSupport = configuration.getJsonSupport();
        this.storeFactory = configuration.getStoreFactory();
        this.exceptionListener = configuration.getExceptionListener();
        this.ackMode = configuration.getAckMode();
    }

    public void addClient(SocketIOClient client) {
        allClients.put(client.getSessionId(), client);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addMultiTypeEventListener(String eventName, MultiTypeEventListener listener,
                                          Class<?>... eventClass) {
        EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            entry = new EventEntry();
            EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName, entry);
            if (oldEntry != null) {
                entry = oldEntry;
            }
        }
        entry.addListener(listener);
        jsonSupport.addEventMapping(name, eventName, eventClass);
    }

    @Override
    public void removeAllListeners(String eventName) {
        EventEntry<?> entry = eventListeners.remove(eventName);
        if (entry != null) {
            jsonSupport.removeEventMapping(name, eventName);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {
        EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            entry = new EventEntry<T>();
            EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName, entry);
            if (oldEntry != null) {
                entry = oldEntry;
            }
        }
        entry.addListener(listener);
        jsonSupport.addEventMapping(name, eventName, eventClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(NamespaceClient client, String eventName, List<Object> args, AckRequest ackRequest) {
        EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            return;
        }

        try {
            Queue<DataListener> listeners = entry.getListeners();
            for (DataListener dataListener : listeners) {
                Object data = getEventData(args, dataListener);
                dataListener.onData(client, data, ackRequest);
            }
        } catch (Exception e) {
            exceptionListener.onEventException(e, args, client);
            if (ackMode == AckMode.AUTO_SUCCESS_ONLY) {
                return;
            }
        }

        sendAck(ackRequest);
    }

    private void sendAck(AckRequest ackRequest) {
        if (ackMode == AckMode.AUTO || ackMode == AckMode.AUTO_SUCCESS_ONLY) {
            // send ack response if it not executed
            // during {@link DataListener#onData} invocation
            ackRequest.sendAckData(Collections.emptyList());
        }
    }

    private Object getEventData(List<Object> args, DataListener<?> dataListener) {
        if (dataListener instanceof MultiTypeEventListener) {
            return new MultiTypeArgs(args);
        } else {
            if (!args.isEmpty()) {
                return args.get(0);
            }
        }
        return null;
    }

    @Override
    public void addDisconnectListener(DisconnectListener listener) {
        disconnectListeners.add(listener);
    }

    public void onDisconnect(SocketIOClient client) {
        try {
            for (DisconnectListener listener : disconnectListeners) {
                listener.onDisconnect(client);
            }
        } catch (Exception e) {
            exceptionListener.onDisconnectException(e, client);
        }


        Set<String> joinedRooms = client.getAllRooms();
        allClients.remove(client.getSessionId());

        leave(getName(), client.getSessionId());
        storeFactory.pubSubStore().publish(PubSubType.LEAVE, new JoinLeaveMessage(client.getSessionId(), getName(), getName()));

        for (String joinedRoom : joinedRooms) {
            leave(roomClients, joinedRoom, client.getSessionId());
        }
        clientRooms.remove(client.getSessionId());
    }

    @Override
    public void addConnectListener(ConnectListener listener) {
        connectListeners.add(listener);
    }

    public void onConnect(SocketIOClient client) {
        join(getName(), client.getSessionId());
        storeFactory.pubSubStore().publish(PubSubType.JOIN, new JoinLeaveMessage(client.getSessionId(), getName(), getName()));

        try {
            for (ConnectListener listener : connectListeners) {
                listener.onConnect(client);
            }
        } catch (Exception e) {
            exceptionListener.onConnectException(e, client);
        }
    }

    @Override
    public void addPingListener(PingListener listener) {
        pingListeners.add(listener);
    }

    public void onPing(SocketIOClient client) {
        try {
            for (PingListener listener : pingListeners) {
                listener.onPing(client);
            }
        } catch (Exception e) {
            exceptionListener.onPingException(e, client);
        }
    }

    @Override
    public BroadcastOperations getBroadcastOperations() {
        return new BroadcastOperations(allClients.values(), storeFactory);
    }

    @Override
    public BroadcastOperations getRoomOperations(String room) {
        return new BroadcastOperations(getRoomClients(room), storeFactory);
    }

    @Override
    public BroadcastOperations getRoomOperations(String room, String excludeUserKeys) {
        return new BroadcastOperations(getRoomClients(room, excludeUserKeys), storeFactory);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        com.corundumstudio.socketio.namespace.Namespace other = (com.corundumstudio.socketio.namespace.Namespace) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public void addListeners(Object listeners) {
        addListeners(listeners, listeners.getClass());
    }

    @Override
    public void addListeners(Object listeners, Class<?> listenersClass) {
        engine.scan(this, listeners, listenersClass);
    }

    public void joinRoom(String room, UUID sessionId) {
        join(room, sessionId);
        storeFactory.pubSubStore().publish(PubSubType.JOIN, new JoinLeaveMessage(sessionId, room, getName()));
    }

    public void joinRoom(String room, UUID sessionId, String userKey) {
        join(room, sessionId, userKey);
        storeFactory.pubSubStore().publish(PubSubType.JOIN, new JoinLeaveMessage(sessionId, room, getName()));
    }

    public void dispatch(String room, Packet packet) {
        Iterable<SocketIOClient> clients = getRoomClients(room);

        final List<String> excludeClients = excludeClients(packet.getExcludeClients(), packet.getExcludeUserKeys());

        for (SocketIOClient socketIOClient : clients) {
            AtomicBoolean shouldSkip = new AtomicBoolean(true);
            socketIOClient.getAllRooms().stream().forEach(innerRoom -> {
                if (innerRoom.equals(room)) {
                    shouldSkip.set(false);
                }
            });

            if (shouldSkip.get()) {
                continue;
            }

            if (excludeClients.contains(socketIOClient.getSessionId().toString())) {
                continue;
            }

            socketIOClient.send(packet);
        }
    }

    private List<String> excludeClients(String excludeClients, String userKeys) {
        List<String> excludeClientsByClients = getExcludeClientsByClients(excludeClients);
        List<String> excludeClientsByUserKey = getExcludeClientsByUserKey(userKeys);

        List<String> excludeClientList = Lists.newArrayList();
        excludeClientList.addAll(excludeClientsByClients);
        excludeClientList.addAll(excludeClientsByUserKey);
        return excludeClientList;
    }

    private List<String> getExcludeClientsByClients(String excludeClients) {
        if (excludeClients == null || excludeClients.length() == 0) {
            return Lists.newArrayList();
        }

        List<String> excludeCliets = Arrays.asList(excludeClients.split(","));
        return excludeCliets;
    }


    private List<String> getExcludeClientsByUserKey(String userKeys) {
        if (userKeys == null || userKeys.length() == 0) {
            return Lists.newArrayList();
        }

        List<String> excludeClietList = Lists.newArrayList();

        final List<String> userKeySplits = Arrays.asList(userKeys.split(","));
        userKeySplits.stream().forEach(userKey -> {
            final Set<UUID> uuids = userKeyClients.get(userKey);
            if (uuids != null && uuids.size() > 0) {
                Arrays.stream(uuids.toArray()).forEach(uuid -> {
                    excludeClietList.add(uuid.toString());
                });
            }
        });

        return excludeClietList;
    }

    private <K, V> void join(ConcurrentMap<K, Set<V>> map, K key, V value) {
        Set<V> clients = map.get(key);
        if (clients == null) {
            clients = Collections.newSetFromMap(PlatformDependent.<V, Boolean>newConcurrentHashMap());
            Set<V> oldClients = map.putIfAbsent(key, clients);
            if (oldClients != null) {
                clients = oldClients;
            }
        }
        clients.add(value);
        // object may be changed due to other concurrent call
        if (clients != map.get(key)) {
            // re-join if queue has been replaced
            join(map, key, value);
        }
    }

    public void join(String room, UUID sessionId) {
        join(roomClients, room, sessionId);
        join(clientRooms, sessionId, room);
    }

    public void join(String room, UUID sessionId, String userKey) {
        join(roomClients, room, sessionId);
        join(clientRooms, sessionId, room);
        join(userKeyClients, userKey, sessionId);
    }

    public void leaveRoom(String room, UUID sessionId) {
        leave(room, sessionId);
        storeFactory.pubSubStore().publish(PubSubType.LEAVE, new JoinLeaveMessage(sessionId, room, getName()));
    }

    public void leaveRoom(String room, UUID sessionId, String userKey) {
        leave(room, sessionId, userKey);
        storeFactory.pubSubStore().publish(PubSubType.LEAVE, new JoinLeaveMessage(sessionId, room, getName()));
    }

    private <K, V> void leave(ConcurrentMap<K, Set<V>> map, K room, V sessionId) {
        Set<V> clients = map.get(room);
        if (clients == null) {
            return;
        }
        clients.remove(sessionId);

        if (clients.isEmpty()) {
            map.remove(room, Collections.emptySet());
        }
    }

    public void leave(String room, UUID sessionId) {
        leave(roomClients, room, sessionId);
        leave(clientRooms, sessionId, room);
    }

    public void leave(String room, UUID sessionId, String userKey) {
        leave(roomClients, room, sessionId);
        leave(clientRooms, sessionId, room);
        leave(userKeyClients, userKey, sessionId);
    }

    public Set<String> getRooms(SocketIOClient client) {
        Set<String> res = clientRooms.get(client.getSessionId());
        if (res == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(res);
    }

    public Set<String> getRooms() {
        return roomClients.keySet();
    }

    public Iterable<SocketIOClient> getRoomClients(String room) {
        Set<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        List<SocketIOClient> result = new ArrayList<SocketIOClient>();
        for (UUID sessionId : sessionIds) {
            SocketIOClient client = allClients.get(sessionId);
            if (client != null) {
                result.add(client);
            }
        }
        return result;
    }

    public Iterable<SocketIOClient> getRoomClients(String room, String excludeUserKeys) {
        Set<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        final List<String> excludeClients = getExcludeClientsByUserKey(excludeUserKeys);

        List<SocketIOClient> result = new ArrayList<SocketIOClient>();
        for (UUID sessionId : sessionIds) {
            SocketIOClient client = allClients.get(sessionId);
            if (client != null && !excludeClients.contains(client.getSessionId().toString())) {
                result.add(client);
            }
        }
        return result;
    }

    @Override
    public Collection<SocketIOClient> getAllClients() {
        return Collections.unmodifiableCollection(allClients.values());
    }

    public JsonSupport getJsonSupport() {
        return jsonSupport;
    }

    @Override
    public SocketIOClient getClient(UUID uuid) {
        return allClients.get(uuid);
    }

}