package org.apxeolog.streamchat.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apxeolog.streamchat.ChatSettings;
import org.apxeolog.streamchat.JSBridge;
import org.apxeolog.streamchat.Utils;
import org.jibble.pircbot.PircBot;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Created by APXEOLOG on 09/07/2015.
 * Visit http://www.twitchapps.com/tmi/ to generate your IRC password
 */
public class TwitchChat extends AbstractChat {
    private static final Logger LOG = LogManager.getLogger(TwitchChat.class);

    private static class TwitchIrcClient extends PircBot {
        public TwitchIrcClient(String login) {
            setName(login);
        }
    }

    public static class OnlineWatcherThread extends Thread {
        private boolean alive = true;
        private AbstractChat chat;

        public OnlineWatcherThread(AbstractChat chat) {
            this.chat = chat;
            setDaemon(true);
            setName("Twitch OnlineWatcher Thread");
        }

        @Override
        public void run() {
            while (alive) {
                String response = Utils.get("https://api.twitch.tv/kraken/streams/" + chat.settings.login.toLowerCase());
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

    private TwitchIrcClient twitchIrcClient;
    private OnlineWatcherThread onlineWatcherThread;

    public TwitchChat(ChatSettings settings) {
        super(settings);
        twitchIrcClient = new TwitchIrcClient(settings.login) {
            @Override
            protected void onMessage(String channel, String sender, String login, String hostname, String message) {
                HashMap<String, String> messageData = new HashMap<>();
                messageData.put("channel", channel);
                messageData.put("sender", sender);
                messageData.put("login", login);
                messageData.put("hostname", hostname);
                messageData.put("message", Charset.forName("UTF-8").decode(ByteBuffer.wrap(message.getBytes())).toString());
                emit("message", Utils.toJson(messageData));
            }
        };
        // twitchIrcClient.setVerbose(true);
    }

    @Override
    public void connect() {
        try {
            twitchIrcClient.connect("irc.twitch.tv", 6667, settings.password);
            twitchIrcClient.joinChannel("#" + settings.login.toLowerCase());
            onlineWatcherThread = new OnlineWatcherThread(this);
            onlineWatcherThread.start();
        } catch (Exception ex) {
            LOG.error("Twitch Chat error", ex);
        }
    }

    @Override
    public void disconnect() {
        if (twitchIrcClient != null) {
            twitchIrcClient.disconnect();
            twitchIrcClient.dispose();
            twitchIrcClient = null;
        }
        if (onlineWatcherThread != null) {
            onlineWatcherThread.stopThread();
            onlineWatcherThread.interrupt();
            onlineWatcherThread = null;
        }
    }
}
