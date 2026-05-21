package server;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        int port = 5000;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Usage: java server.Server [port]");
                return;
            }
        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Server started on port " + port + "...");

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("New client connected");

                ClientHandler clientThread =
                        new ClientHandler(socket, clients);

                clients.add(clientThread);

                clientThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
