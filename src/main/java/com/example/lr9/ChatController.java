package com.example.lr9;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class ChatController {

    @FXML private ComboBox<Integer> protectionLevelBox;
    @FXML private TextField modeDesc;
    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private Label statusLabel;
    @FXML private TextArea chatArea;
    @FXML private ComboBox<String> targetUserBox;
    @FXML private TextField messageField;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentUser;
    private boolean useProxy = false;
    private boolean useTunnel = false;

    @FXML
    public void initialize() {
        protectionLevelBox.getItems().addAll(0, 1, 2, 3);
        protectionLevelBox.setValue(0);
        protectionLevelBox.setOnAction(e -> updateProtectionMode());
        updateProtectionMode();
    }

    private void updateProtectionMode() {
        int level = protectionLevelBox.getValue();
        String mode;
        switch (level) {
            case 0: mode = "🚫 Уровень 0: Без защиты (прямое соединение)"; break;
            case 1: mode = "🛡️ Уровень 1: Прокси (скрытие IP)"; break;
            case 2: mode = "🔒 Уровень 2: Туннель (Protobuf шифрование)"; break;
            case 3: mode = "🛡️🔒 Уровень 3: Прокси + Туннель (полное скрытие)"; break;
            default: mode = "Неизвестно";
        }
        modeDesc.setText(mode);
        appendToChat("📊 " + mode);
    }

    @FXML
    private void onConnect() {
        try {
            int level = protectionLevelBox.getValue();
            useProxy = (level == 1 || level == 3);
            useTunnel = (level == 2 || level == 3);

            int port = useProxy ? 9999 : Integer.parseInt(portField.getText());
            connect(serverField.getText(), port);
            statusLabel.setText("✅ Подключен (Уровень " + level + ")");
            statusLabel.setTextFill(Color.GREEN);
        } catch (Exception ex) {
            appendToChat("❌ Ошибка подключения: " + ex.getMessage());
        }
    }

    @FXML
    private void onDisconnect() {
        disconnect();
    }

    @FXML
    private void onRegister() {
        if (socket != null && !socket.isClosed()) {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                sendCommand("REGISTER|" + username);
            } else {
                appendToChat("❌ Введите имя пользователя");
            }
        } else {
            appendToChat("❌ Сначала подключитесь к серверу");
        }
    }

    @FXML
    private void onSend() {
        if (currentUser != null && !messageField.getText().isEmpty()) {
            String target = targetUserBox.getValue();
            if (target != null && !target.isEmpty() && !target.equals(currentUser)) {
                String msg = messageField.getText();
                if (useTunnel) {
                    msg = encryptMessage(msg);
                }
                sendCommand("SEND|" + target + "|" + msg);
                messageField.clear();
                appendToChat("💌 Анонимное сообщение отправлено пользователю '" + target + "'");
            } else if (target != null && target.equals(currentUser)) {
                appendToChat("❌ Нельзя отправить сообщение самому себе");
            } else {
                appendToChat("❌ Укажите получателя");
            }
        } else if (currentUser == null) {
            appendToChat("❌ Сначала зарегистрируйтесь");
        } else if (messageField.getText().isEmpty()) {
            appendToChat("❌ Введите сообщение");
        }
    }

    @FXML
    private void onInbox() {
        if (currentUser != null) {
            sendCommand("INBOX");
        } else {
            appendToChat("❌ Сначала зарегистрируйтесь");
        }
    }

    private void connect(String host, int port) throws IOException {
        if (socket != null && !socket.isClosed()) {
            disconnect();
        }

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(this::receiveMessages).start();

        appendToChat("🔗 Подключен к " + host + ":" + port);
        if (useProxy) appendToChat("🛡️ Режим PROXY: Ваш IP скрыт от сервера");
        if (useTunnel) appendToChat("🔒 Режим TUNNEL: Сообщения шифруются (Protobuf)");
    }

    private void receiveMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String response = line;
                Platform.runLater(() -> processResponse(response));
            }
        } catch (IOException e) {
            Platform.runLater(() -> appendToChat("⚠️ Соединение потеряно"));
        }
    }

    private void processResponse(String response) {
        String[] parts = response.split("\\|", 2);

        switch (parts[0]) {
            case "OK":
                if (parts.length > 1 && parts[1].contains("зарегистрирован")) {
                    currentUser = usernameField.getText();
                    appendToChat("✅ " + parts[1]);
                    if (!targetUserBox.getItems().contains(currentUser)) {
                        targetUserBox.getItems().add(currentUser);
                    }
                    statusLabel.setText("✅ Зарегистрирован как: " + currentUser);
                    statusLabel.setTextFill(Color.GREEN);
                } else if (parts.length > 1) {
                    appendToChat("✅ " + parts[1]);
                }
                break;

            case "ERROR":
                appendToChat("❌ " + (parts.length > 1 ? parts[1] : "Неизвестная ошибка"));
                break;

            case "NOTIFY":
                appendToChat("🔔 " + (parts.length > 1 ? parts[1] : "Уведомление"));
                break;

            case "MESSAGES":
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                if (count == 0) {
                    appendToChat("📬 У вас нет новых сообщений");
                } else {
                    appendToChat("📬 У вас " + count + " сообщений:");
                }
                break;

            case "END":
                appendToChat("--- Конец списка ---");
                break;

            default:
                if (!response.equals("END") && !response.startsWith("MESSAGES|")) {
                    String decrypted = useTunnel ? decryptMessage(response) : response;
                    appendToChat("💌 " + decrypted);
                }
                break;
        }
    }

    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            System.out.println("📤 Отправлено: " + command);
        }
    }

    private String encryptMessage(String msg) {
        try {
            MessageProtos.ChatMessage protoMsg = MessageProtos.ChatMessage.newBuilder()
                    .setType("MESSAGE")
                    .setContent(msg)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            return Base64.getEncoder().encodeToString(protoMsg.toString().getBytes());
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(msg.getBytes());
        }
    }

    private String decryptMessage(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            String decodedStr = new String(decoded);
            if (decodedStr.contains("content=")) {
                int start = decodedStr.indexOf("content=\"") + 9;
                int end = decodedStr.indexOf("\"", start);
                if (start > 8 && end > start) {
                    return decodedStr.substring(start, end);
                }
            }
            return decodedStr;
        } catch (Exception e) {
            return encrypted;
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            currentUser = null;
            statusLabel.setText("⛔ Отключен");
            statusLabel.setTextFill(Color.RED);
            appendToChat("🔌 Отключен от сервера");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendToChat(String msg) {
        chatArea.appendText(msg + "\n");
    }
}