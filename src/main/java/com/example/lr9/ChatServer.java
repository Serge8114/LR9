package com.example.lr9;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    private static final int PORT = 8888;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, Queue<String>> messageQueues = new ConcurrentHashMap<>();
    private final List<String> messageLog = new ArrayList<>();

    public void start() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     🔐 СЕРВЕР 'ТАЙНЫЙ ПОКЛОННИК' ЗАПУЩЕН                ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Порт: " + PORT + "                                              ║");
        System.out.println("║  Сервер видит реальные IP клиентов                        ║");
        System.out.println("║  (кроме случаев использования прокси)                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ExecutorService pool = Executors.newCachedThreadPool();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                System.out.println("\n📡 Новое подключение от IP: " + clientIp);

                ClientHandler handler = new ClientHandler(clientSocket, this);
                pool.submit(handler);
            }
        } catch (IOException e) {
            System.err.println("❌ Ошибка запуска сервера: " + e.getMessage());
        }
    }

    public synchronized boolean registerUser(String username, ClientHandler handler) {
        if (!clients.containsKey(username)) {
            clients.put(username, handler);
            messageQueues.putIfAbsent(username, new ConcurrentLinkedQueue<>());
            System.out.println("✅ Зарегистрирован новый пользователь: " + username);
            System.out.println("📊 Всего участников онлайн: " + clients.size());
            return true;
        }
        System.out.println("❌ Отказ регистрации: имя '" + username + "' уже занято");
        return false;
    }

    public synchronized boolean sendMessage(String from, String to, String content) {
        if (!messageQueues.containsKey(to)) {
            System.out.println("❌ Сообщение не доставлено: получатель '" + to + "' не найден");
            return false;
        }

        messageQueues.get(to).add(content);
        messageLog.add(String.format("[%tH:%tM:%tS] %s -> %s: %s",
                System.currentTimeMillis(), System.currentTimeMillis(),
                System.currentTimeMillis(), from, to, content));

        System.out.println("📨 Анонимное сообщение доставлено:");
        System.out.println("   Отправитель: " + from + " (анонимно для получателя)");
        System.out.println("   Получатель: " + to);
        System.out.println("   Содержание: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content));

        ClientHandler receiver = clients.get(to);
        if (receiver != null) {
            receiver.sendMessage("NOTIFY|📬 Вам пришло новое анонимное сообщение!");
        }

        return true;
    }

    public synchronized List<String> getMessages(String username) {
        Queue<String> queue = messageQueues.get(username);
        List<String> messages = new ArrayList<>();

        if (queue != null) {
            String msg;
            while ((msg = queue.poll()) != null) {
                messages.add(msg);
            }
            System.out.println("📬 " + username + " получил(а) " + messages.size() + " сообщений");
        }
        return messages;
    }

    public synchronized void disconnectUser(String username) {
        clients.remove(username);
        System.out.println("👋 Пользователь отключен: " + username);
        System.out.println("📊 Осталось участников онлайн: " + clients.size());
    }

    public void showStats() {
        System.out.println("\n📊 СТАТИСТИКА СЕРВЕРА:");
        System.out.println("   👥 Онлайн: " + clients.size());
        System.out.println("   💬 Всего сообщений в логе: " + messageLog.size());
        int totalMessages = messageQueues.values().stream().mapToInt(Queue::size).sum();
        System.out.println("   📨 Ожидает прочтения: " + totalMessages);
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                server.showStats();
            }
        }, 30000, 30000);

        server.start();
    }
}

class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("📥 Команда от " + (username != null ? username : "неизвестный") + ": " + inputLine);

                String[] parts = inputLine.split("\\|", 3);
                String command = parts[0];

                switch (command) {
                    case "REGISTER":
                        if (parts.length > 1) {
                            String newUser = parts[1];
                            if (server.registerUser(newUser, this)) {
                                username = newUser;
                                out.println("OK|" + newUser + " зарегистрирован");
                                System.out.println("✅ Отправлено подтверждение регистрации");
                            } else {
                                out.println("ERROR|Пользователь '" + newUser + "' уже существует");
                            }
                        } else {
                            out.println("ERROR|Не указано имя пользователя");
                        }
                        break;

                    case "SEND":
                        if (username == null) {
                            out.println("ERROR|Сначала зарегистрируйтесь");
                        } else if (parts.length > 2) {
                            String to = parts[1];
                            String content = parts[2];
                            if (server.sendMessage(username, to, content)) {
                                out.println("OK|Сообщение отправлено");
                            } else {
                                out.println("ERROR|Получатель '" + to + "' не найден");
                            }
                        } else {
                            out.println("ERROR|Неверный формат. Используйте: SEND|получатель|сообщение");
                        }
                        break;

                    case "INBOX":
                        if (username == null) {
                            out.println("ERROR|Сначала зарегистрируйтесь");
                        } else {
                            List<String> msgs = server.getMessages(username);
                            out.println("MESSAGES|" + msgs.size());
                            for (String msg : msgs) {
                                out.println(msg);
                            }
                            out.println("END");
                        }
                        break;

                    case "STATS":
                        server.showStats();
                        out.println("OK|Статистика выведена в консоль");
                        break;

                    default:
                        out.println("ERROR|Неизвестная команда: " + command);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Ошибка клиента " + (username != null ? username : "неизвестный") + ": " + e.getMessage());
        } finally {
            if (username != null) {
                server.disconnectUser(username);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }
}