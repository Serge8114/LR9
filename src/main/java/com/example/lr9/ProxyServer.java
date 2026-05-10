package com.example.lr9;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ProxyServer {

    private static final int PROXY_PORT = 9999;
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 8888;
    private int activeConnections = 0;

    public void start() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     🛡️ ПРОКСИ-СЕРВЕР ЗАПУЩЕН                            ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Порт прокси: " + PROXY_PORT + "                                          ║");
        System.out.println("║  Целевой сервер: " + TARGET_HOST + ":" + TARGET_PORT + "                           ║");
        System.out.println("║  Сервер видит IP прокси, а не реальный IP клиента       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        try (ServerSocket proxySocket = new ServerSocket(PROXY_PORT)) {
            ExecutorService pool = Executors.newCachedThreadPool();

            while (true) {
                Socket clientSocket = proxySocket.accept();
                String clientRealIp = clientSocket.getInetAddress().getHostAddress();
                activeConnections++;

                System.out.println("\n🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌");
                System.out.println("🔌 КЛИЕНТ ПОДКЛЮЧИЛСЯ К ПРОКСИ");
                System.out.println("🔌 Реальный IP клиента: " + clientRealIp);
                System.out.println("🔌 Активных соединений: " + activeConnections);
                System.out.println("🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌🔌");

                pool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("❌ Ошибка запуска прокси: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                Socket targetSocket = new Socket(TARGET_HOST, TARGET_PORT);
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));
                PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true)
        ) {
            String proxyIp = targetSocket.getLocalAddress().getHostAddress();
            System.out.println("\n🔄 ПРОКСИ ПОДКЛЮЧАЕТСЯ К СЕРВЕРУ");
            System.out.println("🔄 IP прокси (видимый серверу): " + proxyIp);
            System.out.println("🔄 Порт прокси: " + targetSocket.getLocalPort());

            Thread clientToTarget = new Thread(() -> {
                try {
                    String line;
                    while ((line = clientIn.readLine()) != null) {
                        String logLine = line.length() > 80 ? line.substring(0, 80) + "..." : line;
                        System.out.println("📤 [ПРОКСИ → СЕРВЕР] " + logLine);
                        targetOut.println(line);
                    }
                } catch (IOException e) {
                    // Нормальное закрытие
                }
            });

            Thread targetToClient = new Thread(() -> {
                try {
                    String line;
                    while ((line = targetIn.readLine()) != null) {
                        String logLine = line.length() > 80 ? line.substring(0, 80) + "..." : line;
                        System.out.println("📥 [СЕРВЕР → ПРОКСИ → КЛИЕНТ] " + logLine);
                        clientOut.println(line);
                    }
                } catch (IOException e) {
                    // Нормальное закрытие
                }
            });

            clientToTarget.start();
            targetToClient.start();

            clientToTarget.join();
            targetToClient.join();

        } catch (IOException | InterruptedException e) {
            System.out.println("⚠️ Ошибка прокси: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                activeConnections--;
                System.out.println("\n🔌 КЛИЕНТ ОТКЛЮЧИЛСЯ ОТ ПРОКСИ");
                System.out.println("🔌 Активных соединений: " + activeConnections);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ProxyServer proxy = new ProxyServer();
        proxy.start();
    }
}