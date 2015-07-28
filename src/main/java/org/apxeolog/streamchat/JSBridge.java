package org.apxeolog.streamchat;

import netscape.javascript.JSObject;
import org.apache.logging.log4j.LogManager;
import org.apxeolog.streamchat.chat.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by APXEOLOG on 13/07/2015.
 */
public class JSBridge {
    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(JSBridge.class);

    public static final String CONFIG_FILE = "config.json";
    public static final int ONLINE_POLL_TIME = 1000;

    private static HashMap<String, Class<? extends AbstractChat>> chatImplementations;
    private static HashMap<String, AbstractChat> activeChats = new HashMap<>();
    static {
        chatImplementations = new HashMap<>();
        chatImplementations.put("cybergame", CybergameChat.class);
        chatImplementations.put("twitch", TwitchChat.class);
        chatImplementations.put("goodgame", GoodgameChat.class);
        chatImplementations.put("hitbox", HitboxChat.class);
    }

    private AppConfig config;
    private JSObject handlerObject;

    public AppConfig getConfig() {
        return config;
    }

    public JSBridge() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            config = createDefaultConfig();
            Utils.writeToFile(CONFIG_FILE, Utils.toJson(config));
        } else {
            config = Utils.fromJson(Utils.readFileContent(CONFIG_FILE), AppConfig.class);
        }
    }

    public void init(JSObject callbackObj) {
        handlerObject = callbackObj;
        ArrayList<String> enabledChats = new ArrayList<>();
        for (Map.Entry<String, Class<? extends AbstractChat>> entry : chatImplementations.entrySet()) {
            ChatSettings settings = config.chats.get(entry.getKey());
            if (settings != null && settings.enabled) {
                settings.chatName = entry.getKey();
                try {
                    AbstractChat chat = entry.getValue().getConstructor(ChatSettings.class).newInstance(settings);
                    chat.bind(new AbstractChat.Callback() {
                        @Override
                        public void call(String chatName, String type, String data) {
                            // LOG.debug(String.format("EMIT: %s, %s", chatName, type));
                            handlerObject.call("emit", chatName, type, data);
                        }
                    });
                    activeChats.put(settings.chatName, chat);
                    enabledChats.add(settings.chatName);
                } catch (Exception ex) {
                    LOG.error("JS Chat Setup error", ex);
                }
            }
        }
        handlerObject.call("init", enabledChats.toArray(new String[enabledChats.size()]));
    }

    public void connect() {
        for (AbstractChat chat : activeChats.values()) {
            chat.connect();
        }
    }

    public void shutdown() {
        for (AbstractChat chat : activeChats.values()) {
            chat.disconnect();
        }
        activeChats.clear();
    }

    public void log(String string) {
        System.out.println(string);
    }

    private AppConfig createDefaultConfig() {
        AppConfig appConfig = new AppConfig();
        appConfig.template = "templates/theme_white.html";
        appConfig.debug = false;
        appConfig.chats = new HashMap<>();
        for (String key : chatImplementations.keySet()) {
            appConfig.chats.put(key, new ChatSettings());
        }
        return appConfig;
    }
}
