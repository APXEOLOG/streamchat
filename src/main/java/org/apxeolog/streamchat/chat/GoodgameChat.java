package org.apxeolog.streamchat.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apxeolog.streamchat.ChatSettings;
import org.apxeolog.streamchat.JSBridge;
import org.apxeolog.streamchat.Utils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by APXEOLOG on 08/07/2015.
 */
public class GoodgameChat extends AbstractChat {
    private static final Logger LOG = LogManager.getLogger(GoodgameChat.class);

    public static class GoodgameWSMessage {
        public String type;
        public Map<String, Object> data;

        public GoodgameWSMessage(String type, Object... args) {
            this.type = type;
            data = new HashMap<>();
            for (int i = 0; i < args.length; i+= 2) {
                data.put(args[i].toString(), args[i + 1]);
            }
        }
    }

    public static class OnlineWatcherThread extends Thread {
        private boolean alive = true;
        private GoodgameChat chat;

        public OnlineWatcherThread(GoodgameChat chat) {
            this.chat = chat;
            setDaemon(true);
            setName("Goodgame OnlineWatcher Thread");
        }

        @Override
        public void run() {
            while (alive) {
                chat.getChannelCounters();
                try {
                    Thread.sleep(JSBridge.ONLINE_POLL_TIME);
                } catch (InterruptedException ex) {
                    // Shutdown
                }
            }
        }

        public void stopThread() {
            alive = false;
        }
    }

    private String userId;
    private String authToken;
    private WebSocketClient webSocketClient;
    private String channelId;
    private OnlineWatcherThread onlineWatcherThread;

    public GoodgameChat(ChatSettings settings) {
        super(settings);
    }

    @Override
    public void connect() {
        try {
            // Login into chat
            Map<String, Object> json = (Map<String, Object>) Utils.fromJson(
                    Utils.post("http://goodgame.ru/ajax/chatlogin/", "login", settings.login, "password", settings.password), Object.class);
            if ((Boolean) json.get("result") == true) {
                authToken = String.valueOf(json.get("token"));
                userId = String.valueOf(json.get("user_id"));
                onlineWatcherThread = new OnlineWatcherThread(this);

                webSocketClient = new WebSocketClient(new URI("ws://chat.goodgame.ru:8081/chat/websocket")) {
                    @Override
                    public void onOpen(ServerHandshake handshakedata) {
                        Thread.currentThread().setName("Goodgame WebSocket Thread");
                        // Request channel
                        Map<String, Object> status = (Map<String, Object>) Utils.fromJson(
                            Utils.get("http://goodgame.ru/api/getchannelstatus", "fmt", "json", "id", settings.login), Object.class);
                        userId = status.keySet().iterator().next(); // Channel data stored in the map
                        channelId = ((Map<String, Object>) status.get(userId)).get("stream_id").toString();
                        requestAuth();
                    }

                    @Override
                    public void onMessage(String message) {
                        GoodgameWSMessage parsed = Utils.fromJson(message, GoodgameWSMessage.class);
                        switch (parsed.type) {
                            case "success_auth": joinChannel(channelId);
                                break;
                            case "success_join":
                                // It's OK
                                onlineWatcherThread.start();
                                break;
                            case "message":
                                emit("message", Utils.toJson(parsed.data));
                                break;
                            case "channel_counters":
                                emit("online", Utils.toJson(parsed.data));
                                break;
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        emit("close", "true");
                    }

                    @Override
                    public void onError(Exception ex) {
                        LOG.error("Goodgame WebSocket error", ex);
                    }
                };
                webSocketClient.connect();
            }
        } catch (Exception ex) {
            LOG.error("Goodgame Chat error", ex);
        }
    }

    @Override
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
        if (onlineWatcherThread != null) {
            onlineWatcherThread.stopThread();
            onlineWatcherThread.interrupt();
            onlineWatcherThread = null;
        }
    }

    private void joinChannel(String channelId) {
        send("join", "channel_id", channelId, "hidden", false, "mobile", false);
    }

    private void getChannelCounters() {
        send("get_channel_counters", "channel_id", channelId);
    }

    private void requestAuth() {
        send("auth", "site_id", 1, "user_id", userId, "token", authToken);
    }

    private void send(String type, Object... args) {
        if (webSocketClient != null) {
            String json = Utils.toJson(new GoodgameWSMessage(type, args));
            webSocketClient.send(json);
        }
    }
}
