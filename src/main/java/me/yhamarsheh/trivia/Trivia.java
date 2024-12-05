package me.yhamarsheh.trivia;

import me.yhamarsheh.trivia.enums.Operation;
import me.yhamarsheh.trivia.objects.Game;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Trivia {

    public static void main(String[] args) throws SocketException, UnknownHostException {
        Scanner sc = new Scanner(System.in);

        System.out.print("Please enter the ip address of the host: ");
        String ip = sc.next();

        System.out.print("Please enter the port of the host: ");
        int port = sc.nextInt();

        System.out.print("Please enter your name: ");
        String name = sc.next();

        Game game = new Game(ip, port, name);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                game.sendMessageToServer(String.format(Operation.QUIT.getFormat(), name));
            } catch (IOException e) {
                System.out.println("Couldn't send the quit message to the server.");
            }
            System.out.println("\n\nExiting the game.");
        }));

        game.join();
    }
}
