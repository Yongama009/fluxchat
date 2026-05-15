package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        try {

            Socket socket = new Socket("localhost", 5000);

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(), true);

            Scanner scanner = new Scanner(System.in);

            // Thread for receiving messages
            new Thread(() -> {

                String serverMessage;

                try {

                    while ((serverMessage = in.readLine()) != null) {

                        System.out.println(serverMessage);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();

            // Sending messages
            while (true) {

                String message = scanner.nextLine();

                out.println(message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}