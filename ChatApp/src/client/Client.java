package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = 5000;

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Usage: java client.Client [host] [port]");
                return;
            }
        }

        try {

            Socket socket = new Socket(host, port);

            System.out.println("Connected to " + host + ":" + port);

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);

            Thread receiver = new Thread(() -> {

                String serverMessage;

                try {

                    while ((serverMessage = in.readLine()) != null) {

                        System.out.println(serverMessage);
                    }

                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.out.println("Disconnected: " + e.getMessage());
                    }
                }

            });
            receiver.setDaemon(true);
            receiver.start();

            System.out.println("Type your name when asked by the server.");
            System.out.println("Try /help, /profile, /users, /adduser, /post, /jobs, or /apply.");

            while (scanner.hasNextLine()) {

                String message = scanner.nextLine();

                out.println(message);
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
