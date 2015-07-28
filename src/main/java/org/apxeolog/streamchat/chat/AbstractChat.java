package org.apxeolog.streamchat.chat;

import org.apxeolog.streamchat.ChatSettings;

import java.util.ArrayList;

/**
 * Created by APXEOLOG on 08/07/2015.
 */
public abstract class AbstractChat {
    public interface Callback {
        void call(String chatName, String type, String data);
    }

    private ArrayList<Callback> listeners;
    protected ChatSettings settings;

    public AbstractChat(ChatSettings settings) {
        this.listeners = new ArrayList<>();
        this.settings = settings;
    }

    /**
     * This method will be called when (and if) chat connection should be initialized
     */
    public abstract void connect();

    /**
     * This method called when program is shutting down, or we need to reconnect
     */
    public abstract void disconnect();

    /**
     *  This method should be used to attach listeners to the events
     *  Default events are:
     *  message - When chat message is received
     */
    public void bind(Callback callback) {
        listeners.add(callback);
    }

    /**
     * This method should be used from implementations to emit event
     * @param event
     * @param data
     */
    protected void emit(String event, String data) {
        for (Callback callback : listeners)
            callback.call(settings.chatName, event, data);
    }
}
