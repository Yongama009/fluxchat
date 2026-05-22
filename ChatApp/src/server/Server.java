package server;

import server.store.AppRepository;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        int port = 5000;
        Path dataFile = Path.of("ChatApp", "data", "fluxchat.db");

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Usage: java server.Server [port]");
                return;
            }
        }

        if (args.length > 1) {
            dataFile = Path.of(args[1]);
        }

        AppRepository repository = new AppRepository(dataFile);

        try {
            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Server started on port " + port + "...");
            System.out.println("Data file: " + dataFile.toAbsolutePath());

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("New client connected");

                ClientHandler clientThread =
                        new ClientHandler(socket, clients, repository);

                clients.add(clientThread);

                clientThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
