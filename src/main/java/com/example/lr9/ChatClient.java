package com.example.lr9;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClient extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/lr9/chat-view.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setTitle("💌 Тайный поклонник - Анонимные сообщения");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}