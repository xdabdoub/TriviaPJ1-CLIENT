package me.yhamarsheh.trivia.objects;

import me.yhamarsheh.trivia.enums.Operation;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.*;

public class Game {

    private final String NAME;
    private final DatagramSocket SOCKET;
    private final InetAddress ADDRESS;
    private final int PORT;

    private boolean running;

    public Game(String hostAddress, int port, String name) throws SocketException, UnknownHostException {
        this.SOCKET = new DatagramSocket();
        this.ADDRESS = InetAddress.getByName(hostAddress);
        this.PORT = port;
        this.NAME = name;

        System.out.println("Registered the Trivia Game server at IP: " + ADDRESS.getHostAddress() + ", Port: " + PORT + ".");
    }

    public void listenForMessages() throws IOException {
        Scanner sc = new Scanner(System.in);

        while (running) {
            byte[] receiveBuffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            SOCKET.receive(packet);

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(receiveBuffer, receiveBuffer.length, address, port);
            String received = new String(receiveBuffer, 0, packet.getLength(), StandardCharsets.UTF_8).trim();

            String[] data = received.split(Operation.SEPARATOR.toString());
            Operation operation;

            try {
                operation = Operation.valueOf(data[0]);
            } catch (IllegalArgumentException e) {
                sendMessageToServer("Invalid Operation Type: " + data[0] + ". More: " + e.getMessage());
                continue;
            }

            switch (operation) {
                case REQUEST_INPUT -> {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<String> future = executor.submit(() -> {
                        String request = data[1];
                        System.out.println(request);

                        return sc.nextLine();
                    });

                    String answer = null;
                    try {
                        answer = future.get(30, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        future.cancel(true);
                    } catch (InterruptedException | ExecutionException ignored) {
                    }

                    if (answer == null) continue;
                    if (answer.isEmpty()) answer = "EMPTY-ANSWER";

                    if (answer.equalsIgnoreCase("exit")) {
                        running = false;
                        System.out.println("Goodbye!");
                        sendMessageToServer(String.format(Operation.QUIT.getFormat(), NAME));
                        return;
                    }

                    System.out.println("Answer submitted: " + answer);
                    System.out.println();
                    sendMessageToServer(String.format(Operation.ANSWER.getFormat(), answer));

                    break;
                }
                case MESSAGE -> {
                    String message = data[1];
                    if (message.contains("You were kicked")) {
                        running = false;
                        return;
                    }

                    if (message.equalsIgnoreCase("BLANK")) System.out.println();
                    else System.out.println(message);
                    break;
                }
                case QUIT -> {
                    String message = data[1];
                    System.out.println(message);

                    running = false;
                }
            }
        }

        SOCKET.close();
        sc.close();
    }

    public void join() throws UnknownHostException {
        // JOIN;NAME
        final String data = Operation.JOIN.name() + ";" + NAME;

        try {
            sendMessageToServer(data);
            running = true;
            listenForMessages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToServer(String message) throws IOException {
        byte[] sendBuffer = message.getBytes(StandardCharsets.UTF_8);

        SOCKET.send(new DatagramPacket(sendBuffer, sendBuffer.length,
                getAddress(), PORT));
    }

    public DatagramSocket getSocket() {
        return SOCKET;
    }

    public InetAddress getAddress() {
        return ADDRESS;
    }

    public int getPort() {
        return PORT;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
