package org.apxeolog.streamchat.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apxeolog.streamchat.ChatSettings;
import org.apxeolog.streamchat.Utils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.rmi.CORBA.Util;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Created by APXEOLOG on 08/07/2015.
 */
public class HitboxChat extends AbstractChat {
    private static final Logger LOG = LogManager.getLogger(HitboxChat.class);

    private static class AuthTokenResponse {
        public String authToken;
    }

    private static class ChatServersResponseEntry {
        public String server_ip;
    }

    private static class HitboxWSMessage {
        public static class HitboxArgumentsEntry {
            public String method;
            public HashMap<String, Object> params;
        }

        public String name;
        public HitboxArgumentsEntry[] args;

        public HitboxWSMessage(String method, Object... parameters) {
            name = "message";
            args = new HitboxArgumentsEntry[1];
            args[0] = new HitboxArgumentsEntry();
            args[0].method = method;
            args[0].params = new HashMap<>();
            for (int i = 0; i < parameters.length; i+= 2) {
                args[0].params.put(parameters[i].toString(), parameters[i + 1]);
            }
        }
    }

    private static class HitboxWSInputMessage {
        public String name;
        public String[] args;
    }

    private static final String API_ENDPOINT = "https://api.hitbox.tv";

    private String authToken;
    private ChatServersResponseEntry[] servers;
    private int serverIndex = 0;
    private WebSocketClient webSocketClient;

    public HitboxChat(ChatSettings settings) {
        super(settings);
    }

    @Override
    public void connect() {
        // Get auth token
        String authTokenRaw = Utils.post(API_ENDPOINT + "/auth/token", "login", settings.login, "pass", settings.password, "app", "desktop");
        if (authTokenRaw.isEmpty()) throw new RuntimeException("Wrong /auth/token response");
        AuthTokenResponse authTokenResponse = Utils.fromJson(authTokenRaw, AuthTokenResponse.class);
        if (authTokenResponse.authToken == null || authTokenResponse.authToken.isEmpty()) throw new RuntimeException("Empty /auth/token response data");
        authToken = authTokenResponse.authToken;
        // Get server list
        String chatServersRaw = Utils.get(API_ENDPOINT + "/chat/servers");
        if (chatServersRaw.isEmpty()) throw new RuntimeException("Wrong /chat/servers response");
        servers = Utils.fromJson(chatServersRaw, ChatServersResponseEntry[].class);
        // Pick one server and connect
        try {
            connectToServer(servers[serverIndex].server_ip);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    private void invokeMethod(String method, Object... args) {
        if (webSocketClient != null) {
            String message = "5:::" + Utils.toJson(new HitboxWSMessage(method, args));
            webSocketClient.send(message);
        }
    }

    private void joinChannel() {
        invokeMethod("joinChannel",
                "channel", settings.login.toLowerCase(),
                "name", settings.login
        );
    }

    private void dispatchMessage(String message) {
        HitboxWSInputMessage parsed = Utils.fromJson(message.substring(4), HitboxWSInputMessage.class);
        for (int i = 0; i < parsed.args.length; i++) {
            HitboxWSMessage.HitboxArgumentsEntry entry = Utils.fromJson(parsed.args[i], HitboxWSMessage.HitboxArgumentsEntry.class);
            if (entry.method.equals("chatMsg"))
                emit("message", Utils.toJson(entry.params));
            else if (entry.method.equals("infoMsg"))
                emit("online", Utils.toJson(entry.params));
            else LOG.debug(entry.method);
        }
    }

    private void connectToServer(String serverIp) throws URISyntaxException {
        String serverSocketRaw = Utils.get("http://" + serverIp + "/socket.io/1/");
        String webSocketId = serverSocketRaw.substring(0, serverSocketRaw.indexOf(':'));
        URI webSocketUri = new URI("ws://" + serverIp + "/socket.io/1/websocket/" + webSocketId);
        webSocketClient = new WebSocketClient(webSocketUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Thread.currentThread().setName("Hitbox WebSocket Thread");
            }

            @Override
            public void onMessage(String message) {
                if (message.equals("1::")) {
                    // Join channel
                    joinChannel();
                } else if (message.equals("2::")) {
                    // We are alive
                    webSocketClient.send("2::");
                } else {
                    dispatchMessage(message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                emit("close", "true");
            }

            @Override
            public void onError(Exception ex) {
                LOG.error("Hitbox WebSocket error", ex);
            }
        };
        webSocketClient.connect();
    }
}
