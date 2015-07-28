package org.apxeolog.streamchat.chat;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apxeolog.streamchat.ChatSettings;
import org.apxeolog.streamchat.JSBridge;
import org.apxeolog.streamchat.Utils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by APXEOLOG on 09/07/2015.
 * Worst protocol i've ever seen
 */
public class CybergameChat extends AbstractChat {
    private static final Logger LOG = LogManager.getLogger(CybergameChat.class);

    public static class CybergameWSMessage {
        public String command;
        public Map<String, Object> message;

        public CybergameWSMessage(String type, Object... args) {
            this.command = type;
            message = new HashMap<>();
            for (int i = 0; i < args.length; i+= 2) {
                message.put(args[i].toString(), args[i + 1]);
            }
        }
    }

    public static class CybergameWSSingleMessage {
        public String command;
        public String message;

        public CybergameWSSingleMessage(String type, String message) {
            this.command = type;
            this.message = message;
        }
    }

    public static class OnlineWatcherThread extends Thread {
        private boolean alive = true;
        private AbstractChat chat;

        public OnlineWatcherThread(AbstractChat chat) {
            this.chat = chat;
            setDaemon(true);
            setName("Cybergame OnlineWatcher Thread");
        }

        @Override
        public void run() {
            while (alive) {
                String response = Utils.get("http://api.cybergame.tv/w/streams2.php?channel=" + chat.settings.login.toLowerCase());
                chat.emit("online", response);
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

    private String authName;
    private String authToken;
    private String authIpb;
    private WebSocketClient webSocketClient;
    private OnlineWatcherThread onlineWatcherThread;

    public CybergameChat(ChatSettings settings) {
        super(settings);
    }

    private String getCookieString() {
        return String.format("kname=%s; khash=%s; kipb=%s", authName, authToken, authIpb);
    }

    private String getSockJSUrl() {
        return String.format("%d/%s/websocket", (int)(Math.random() * 900 + 1), UUID.randomUUID().toString().substring(0, 8));
    }

    private void auth() {
        HashMap<String, String> map = new HashMap<>();
        map.put("login", authName);
        map.put("password", authToken);
        map.put("channel", "#" + settings.login.toLowerCase());
        send("login", Utils.toJson(map));
    }

    private void getUsers() {
        HashMap<String, String> map = new HashMap<>();
        map.put("channel", "#" + settings.login.toLowerCase());
        send("getUsers", Utils.toJson(map));
    }

    private void send(String command, Object... params) {
        String encoded;
        if (params.length == 1) encoded = Utils.toJson(new CybergameWSSingleMessage(command, (String)params[0]));
        else encoded = Utils.toJson(new CybergameWSMessage(command, params));
        String json = "[" + Utils.toJson(encoded) + "]";
        webSocketClient.send(json);
    }

    @Override
    public void connect() {
        try {
            // Login via site
            HttpContext context = new BasicHttpContext();
            Utils.post("http://cybergame.tv/login.php", context, "action", "login", "username", settings.login, "pass", settings.password);
            String loginResponseBody = Utils.post("http://cybergame.tv", context);
            // Get tokens from page body
            Matcher matcher = Pattern.compile("kname = \"([^\"]+)\";").matcher(loginResponseBody);
            if (matcher.find()) {
                authName = matcher.group(1);
            }
            matcher = Pattern.compile("khash = \"([^\"]+)\";").matcher(loginResponseBody);
            if (matcher.find()) {
                authToken = matcher.group(1);
            }
            matcher = Pattern.compile("kipb = \"([^\"]+)\";").matcher(loginResponseBody);
            if (matcher.find()) {
                authIpb = matcher.group(1);
            }

            HashMap<String, String> httpHeaders = new HashMap<>();
            httpHeaders.put("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");
            webSocketClient = new WebSocketClient(new URI("ws://cybergame.tv:9090/" + getSockJSUrl()), new Draft_17(), httpHeaders, 5000) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Thread.currentThread().setName("Cybergame WebSocket Thread");
                }

                @Override
                public void onMessage(String message) {
                    if (message.equals("o")) {
                        // SockJS Open frame - connection OK
                        auth();
                    } else if (message.equals("h")) {
                        // SockJS Heartbeat frame
                        getUsers(); // Just ask for users to keep us live
                    } else if (message.startsWith("a")) {
                        // Message
                        try {
                            CybergameWSMessage parsed = Utils.fromJson(Utils.fromJson(message.substring(2, message.length() - 1), String.class), CybergameWSMessage.class);
                        } catch (Exception ex) {
                            CybergameWSSingleMessage parsed = Utils.fromJson(Utils.fromJson(message.substring(2, message.length() - 1), String.class), CybergameWSSingleMessage.class);
                            switch (parsed.command) {
                                case "chatMessage":
                                    emit("message", parsed.message);
                                    break;
                            }
                        }
                    } else if (message.startsWith("c")) {
                        emit("close", "true");
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    emit("close", "true");
                }

                @Override
                public void onError(Exception ex) {
                    LOG.error("Cybergame WebSocket error", ex);
                }
            };
            webSocketClient.connect();
            onlineWatcherThread = new OnlineWatcherThread(this);
            onlineWatcherThread.start();
        } catch (Exception ex) {
            LOG.error("Cybergame Chat error", ex);
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
}
