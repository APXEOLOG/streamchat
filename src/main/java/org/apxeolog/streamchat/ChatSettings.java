package org.apxeolog.streamchat;

/**
 * Created by APXEOLOG on 09/07/2015.
 */
public class ChatSettings {
    public String login;
    public String password;
    public boolean enabled;

    public transient String chatName;

    public ChatSettings() {
        login = "";
        password = "";
        enabled = false;
    }
}
