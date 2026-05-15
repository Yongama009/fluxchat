package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {

    private static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);

            System.out.println("Server started...");

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