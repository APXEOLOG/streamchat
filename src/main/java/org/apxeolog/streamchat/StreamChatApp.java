package org.apxeolog.streamchat;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import netscape.javascript.JSObject;

import java.io.File;

/**
 * Created by APXEOLOG on 09/07/2015.
 */
public class StreamChatApp extends Application {
    private WebEngine webEngine;
    private JSBridge jsBridge;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Transparent window, no borders
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        StackPane stackPane = new StackPane();
        // Default size, TODO: Load from settings
        Scene scene = new Scene(stackPane, 400, 300);
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        stackPane.getChildren().add(webView);
        primaryStage.setScene(scene);
        // This is invisible anyway :)
        primaryStage.setTitle("StreamChat by APXEOLOG");
        primaryStage.show();
        jsBridge = new JSBridge();
        webEngine.setJavaScriptEnabled(true);
        JSObject windowObject = (JSObject) webEngine.executeScript("window");
        windowObject.setMember("bridge", jsBridge);
        webEngine.load("file:///" + getCurrentPath() + "/" + jsBridge.getConfig().template);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                jsBridge.shutdown();
            }
        });
    }

    private String getCurrentPath() {
        return new File("").getAbsolutePath().toString().replaceAll("\\\\", "/");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
