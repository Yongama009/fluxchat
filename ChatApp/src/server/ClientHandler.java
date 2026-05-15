package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private ArrayList<ClientHandler> clients;

    public ClientHandler(Socket socket,
                         ArrayList<ClientHandler> clients) {

        this.socket = socket;
        this.clients = clients;

        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message) {

        for (ClientHandler client : clients) {
            client.out.println(message);
        }
    }

    @Override
    public void run() {

        String message;

        try {

            while ((message = in.readLine()) != null) {

                System.out.println(message);

                broadcastMessage(message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}