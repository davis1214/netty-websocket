/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.annotation;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.AnnotationScanner;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.handler.SocketIOException;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.namespace.Namespace;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OnConnectScanner implements AnnotationScanner {

    @Override
    public Class<? extends Annotation> getScanAnnotation() {
        return OnConnect.class;
    }

    @Override
    public void addListener(Namespace namespace, final Object object, final Method method, Annotation annotation) {
        namespace.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                try {
                    method.invoke(object, client);
                } catch (InvocationTargetException e) {
                    throw new SocketIOException(e.getCause());
                } catch (Exception e) {
                    throw new SocketIOException(e);
                }
            }
        });
    }

    @Override
    public void validate(Method method, Class<?> clazz) {
        if (method.getParameterTypes().length != 1) {
            throw new IllegalArgumentException("Wrong OnConnect listener signature: " + clazz + "." + method.getName());
        }
        boolean valid = false;
        for (Class<?> eventType : method.getParameterTypes()) {
            if (eventType.equals(SocketIOClient.class)) {
                valid = true;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Wrong OnConnect listener signature: " + clazz + "." + method.getName());
        }
    }

}
