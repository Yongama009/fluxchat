package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler extends Thread {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final AtomicInteger NEXT_JOB_ID = new AtomicInteger(1);
    private static final List<JobPost> JOBS =
            Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, UserProfile> USERS =
            new ConcurrentHashMap<>();

    private final Socket socket;
    private final List<ClientHandler> clients;
    private BufferedReader in;
    private PrintWriter out;
    private String displayName = "Guest";
    private String profile = "No profile yet";

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;

        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

        } catch (IOException e) {
            System.out.println("Could not create client streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            askForDisplayName();
            sendWelcome();
            broadcastSystemMessage(displayName + " joined the opportunity network.");

            String message;
            while ((message = in.readLine()) != null) {
                handleMessage(message.trim());
            }
        } catch (IOException e) {
            System.out.println(displayName + " disconnected.");
        } finally {
            clients.remove(this);
            closeSocket();
            broadcastSystemMessage(displayName + " left the opportunity network.");
        }
    }

    private void askForDisplayName() throws IOException {
        out.println("Enter your name:");
        String name = in.readLine();

        if (name != null && !name.trim().isEmpty()) {
            displayName = name.trim();
        }

        USERS.putIfAbsent(displayName, new UserProfile(displayName));
    }

    private void sendWelcome() {
        out.println("Welcome, " + displayName + ".");
        out.println("FluxChat is now focused on job opportunities and career networking.");
        sendHelp();
    }

    private void handleMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        if (message.equalsIgnoreCase("/help")) {
            sendHelp();
        } else if (message.toLowerCase().startsWith("/profile ")) {
            updateProfile(message.substring("/profile ".length()).trim());
        } else if (message.toLowerCase().startsWith("/adduser ")) {
            addUser(message.substring("/adduser ".length()).trim());
        } else if (message.equalsIgnoreCase("/users")) {
            sendUsers();
        } else if (message.equalsIgnoreCase("/jobs")) {
            sendJobs();
        } else if (message.toLowerCase().startsWith("/post ")) {
            createJobPost(message.substring("/post ".length()).trim());
        } else if (message.toLowerCase().startsWith("/apply ")) {
            applyForJob(message.substring("/apply ".length()).trim());
        } else {
            broadcastMessage("[" + now() + "] " + displayName + ": " + message);
        }
    }

    private void updateProfile(String profileText) {
        if (profileText.isEmpty()) {
            out.println("Usage: /profile Role | skills | location");
            return;
        }

        profile = profileText;
        UserProfile user = USERS.computeIfAbsent(displayName, UserProfile::new);
        user.updateFromProfileText(profileText);

        out.println("Profile updated: " + profile);
        broadcastSystemMessage(displayName + " updated their profile: " + profile);
    }

    private void addUser(String text) {
        String[] parts = text.split("\\|", 4);

        if (parts.length < 4) {
            out.println("Usage: /adduser Name | role | skills | location");
            return;
        }

        UserProfile user = new UserProfile(clean(parts[0]));
        user.role = clean(parts[1]);
        user.skills = clean(parts[2]);
        user.location = clean(parts[3]);

        USERS.put(user.name, user);
        broadcastSystemMessage(displayName + " added user " + user.name
                + " (" + user.role + ", " + user.location + ").");
    }

    private void sendUsers() {
        if (USERS.isEmpty()) {
            out.println("No users have been added yet.");
            return;
        }

        out.println("Users:");
        for (UserProfile user : USERS.values()) {
            out.println("- " + user.name + " | " + user.role
                    + " | " + user.skills + " | " + user.location);
        }
    }

    private void createJobPost(String text) {
        String[] parts = text.split("\\|", 4);

        if (parts.length < 4) {
            out.println("Usage: /post Job title | company | location | description");
            return;
        }

        JobPost job = new JobPost(
                NEXT_JOB_ID.getAndIncrement(),
                clean(parts[0]),
                clean(parts[1]),
                clean(parts[2]),
                clean(parts[3]),
                displayName
        );

        JOBS.add(job);
        broadcastMessage("[JOB #" + job.id + "] " + job.title + " at "
                + job.company + " - " + job.location + " (posted by "
                + job.poster + ")");
    }

    private void sendJobs() {
        synchronized (JOBS) {
            if (JOBS.isEmpty()) {
                out.println("No opportunities have been posted yet.");
                return;
            }

            out.println("Open opportunities:");
            for (JobPost job : JOBS) {
                out.println("#" + job.id + " " + job.title + " at "
                        + job.company + " - " + job.location);
                out.println("   " + job.description);
                out.println("   Posted by " + job.poster + ". Apply with /apply "
                        + job.id + " Your message");
            }
        }
    }

    private void applyForJob(String text) {
        String[] parts = text.split("\\s+", 2);

        if (parts.length < 2) {
            out.println("Usage: /apply JobId Short application message");
            return;
        }

        int jobId;
        try {
            jobId = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            out.println("Job id must be a number.");
            return;
        }

        JobPost job = findJob(jobId);
        if (job == null) {
            out.println("No job found with id #" + jobId + ".");
            return;
        }

        broadcastMessage("[APPLICATION] " + displayName + " applied for #"
                + job.id + " " + job.title + " at " + job.company
                + ": " + parts[1]);
    }

    private JobPost findJob(int jobId) {
        synchronized (JOBS) {
            for (JobPost job : JOBS) {
                if (job.id == jobId) {
                    return job;
                }
            }
        }

        return null;
    }

    private void sendHelp() {
        out.println("Commands:");
        out.println("/profile Role | skills | location");
        out.println("/adduser Name | role | skills | location");
        out.println("/users");
        out.println("/post Job title | company | location | description");
        out.println("/jobs");
        out.println("/apply JobId Short application message");
        out.println("/help");
        out.println("Send any other text as a public networking message.");
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.out.println(message);
        }
        System.out.println(message);
    }

    private void broadcastSystemMessage(String message) {
        broadcastMessage("[SYSTEM] " + message);
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not close socket: " + e.getMessage());
        }
    }

    private String clean(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private static class JobPost {
        private final int id;
        private final String title;
        private final String company;
        private final String location;
        private final String description;
        private final String poster;

        private JobPost(int id,
                        String title,
                        String company,
                        String location,
                        String description,
                        String poster) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.location = location;
            this.description = description;
            this.poster = poster;
        }
    }

    private static class UserProfile {
        private final String name;
        private String role = "No role yet";
        private String skills = "No skills yet";
        private String location = "No location yet";

        private UserProfile(String name) {
            this.name = name;
        }

        private void updateFromProfileText(String profileText) {
            String[] parts = profileText.split("\\|", 3);

            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                role = parts[0].trim();
            }

            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                skills = parts[1].trim();
            }

            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                location = parts[2].trim();
            }
        }
    }
}
