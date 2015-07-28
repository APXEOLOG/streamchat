package org.apxeolog.streamchat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apxeolog.streamchat.chat.HitboxChat;
import org.apxeolog.streamchat.chat.TwitchChat;
import org.java_websocket.exceptions.InvalidDataException;

import java.io.UnsupportedEncodingException;

/**
 * Created by APXEOLOG on 08/07/2015.
 */
public class Main {
    private static final Logger LOG = LogManager.getLogger(HitboxChat.class);

    public static void main(String[] _args) throws UnsupportedEncodingException, InvalidDataException {
        /*.HitboxSettings settings = new HitboxChat.HitboxSettings();
        settings.login = "APXEOLOG";
        settings.password = "121212";
        LOG.debug("LOG PLEASE");
        System.out.println(Main.class.getClassLoader().getResource("."));
        HitboxChat chat = new HitboxChat(settings);
        chat.connect();*/
        /*GoodgameChat.GoodgameSettings settings = new GoodgameChat.GoodgameSettings();
        settings.login = "APXEOLOG";
        settings.password = "121212";
        GoodgameChat chat = new GoodgameChat(settings);
        chat.connect();*/
        /*String hex = "81 FE 00 A8 72 3C 41 46 29 1E 3A 1A 50 5F 2E 2B 1F 5D 2F 22 2E 1E 7B 1A 50 50 2E 21 1B 52 1D 64 5E 60 63 2B 17 4F 32 27 15 59 1D 64 48 60 63 3D 2E 60 1D 64 1E 53 26 2F 1C 60 1D 1A 50 06 1D 1A 2E 1E 00 16 2A 79 0E 0A 3D 7B 1D 1A 2E 1E 6D 1A 2E 60 63 36 13 4F 32 31 1D 4E 25 1A 2E 60 63 7C 2E 60 1D 64 24 53 3B 35 3A 09 20 24 38 73 18 10 18 46 2F 2A 31 77 02 13 1C 45 31 22 39 0E 03 7F 15 51 23 75 2E 60 1D 64 5E 60 1D 1A 50 5F 29 27 1C 52 24 2A 2E 60 1D 64 48 60 1D 1A 50 1F 20 36 0A 59 2E 2A 1D 5B 1D 1A 2E 1E 3C 1A 50 41 63 1B";
        byte[] bytes = DatatypeConverter.parseHexBinary(hex.replaceAll(" ", ""));
        Draft_17 draft = new Draft_17();
        try {
            List<Framedata> framedata = draft.translateFrame(ByteBuffer.wrap(bytes));
            System.out.println(new String(framedata.get(0).getPayloadData().array(), "UTF8"));
            int z = 0;
        } catch (Exception ex) {

        }
        //String gg = "[\"\"{\\\"command\\\":\\\"login\\\",\\\"message\\\":\\\"{\\\\\\\"login\\\\\\\":\\\\\\\"123\\\\\\\",\\\\\\\"password\\\\\\\":\\\\\\\"1234\\\\\\\",\\\\\\\"channel\\\\\\\":\\\\\\\"#123\\\\\\\"}\\\"}\"\"]";
        //System.out.println(new String(bytes, Charset.forName("UTF-8")));
        if (true) return;*/
        /*CybergameChat.CybergameSettings settings = new CybergameChat.CybergameSettings();
        settings.login = "APXEOLOG";
        settings.password = "121212";
        CybergameChat cybergameChat = new CybergameChat(settings);
        cybergameChat.connect();*/
        /*ChatSettings settings = new ChatSettings();
        settings.login = "APXEOLOG";
        settings.password = "oauth:ps6m806bji7buxrz0kvgcwdf9s9u1l";
        TwitchChat twitchChat = new TwitchChat(settings);
        twitchChat.connect();*/
        StreamChatApp.launch(_args);
    }
}
