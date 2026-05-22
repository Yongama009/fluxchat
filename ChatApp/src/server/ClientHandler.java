package server;

import server.store.AppRepository;
import server.store.JobApplication;
import server.store.JobPost;
import server.store.PasswordHasher;
import server.store.UserProfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ClientHandler extends Thread {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final Socket socket;
    private final List<ClientHandler> clients;
    private final AppRepository repository;
    private BufferedReader in;
    private PrintWriter out;
    private String displayName = "Guest";
    private boolean authenticated;

    public ClientHandler(Socket socket, List<ClientHandler> clients, AppRepository repository) {
        this.socket = socket;
        this.clients = clients;
        this.repository = repository;

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
            displayName = clean(name);
        }

        UserProfile user = repository.getOrCreateUser(displayName);
        authenticated = !user.hasPassword();
    }

    private void sendWelcome() {
        out.println("Welcome, " + displayName + ".");
        out.println("FluxChat is now focused on job opportunities and career networking.");
        repository.findUser(displayName)
                .filter(UserProfile::hasPassword)
                .ifPresent(user -> out.println("This name is registered. Use /login password before changing data."));
        sendHelp();
    }

    private void handleMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        if (message.equalsIgnoreCase("/help")) {
            sendHelp();
        } else if (message.toLowerCase().startsWith("/register ")) {
            register(message.substring("/register ".length()).trim());
        } else if (message.toLowerCase().startsWith("/login ")) {
            login(message.substring("/login ".length()).trim());
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
        } else if (message.toLowerCase().startsWith("/applications ")) {
            sendApplications(message.substring("/applications ".length()).trim());
        } else {
            broadcastMessage("[" + now() + "] " + displayName + ": " + message);
        }
    }

    private void register(String password) {
        if (password.length() < 6) {
            out.println("Password must be at least 6 characters.");
            return;
        }

        UserProfile user = repository.getOrCreateUser(displayName);
        if (user.hasPassword()) {
            out.println("This name is already registered. Use /login password.");
            return;
        }

        user.setPasswordHash(PasswordHasher.hash(password));
        repository.saveUser(user);
        authenticated = true;
        out.println("Account registered for " + displayName + ".");
    }

    private void login(String password) {
        Optional<UserProfile> user = repository.findUser(displayName);
        if (user.isEmpty() || !user.get().hasPassword()) {
            out.println("This name is not registered yet. Use /register password.");
            return;
        }

        if (!PasswordHasher.verify(password, user.get().getPasswordHash())) {
            out.println("Login failed.");
            return;
        }

        authenticated = true;
        out.println("Logged in as " + displayName + ".");
    }

    private void updateProfile(String profileText) {
        if (!canChangeData()) {
            return;
        }

        if (profileText.isEmpty()) {
            out.println("Usage: /profile Role | skills | location");
            return;
        }

        UserProfile user = repository.getOrCreateUser(displayName);
        user.updateFromProfileText(profileText);
        repository.saveUser(user);

        out.println("Profile updated: " + profileText);
        broadcastSystemMessage(displayName + " updated their profile: " + profileText);
    }

    private void addUser(String text) {
        if (!canChangeData()) {
            return;
        }

        String[] parts = text.split("\\|", 4);

        if (parts.length < 4) {
            out.println("Usage: /adduser Name | role | skills | location");
            return;
        }

        UserProfile user = new UserProfile(clean(parts[0]));
        user.update(clean(parts[1]), clean(parts[2]), clean(parts[3]));
        repository.saveUser(user);
        broadcastSystemMessage(displayName + " added user " + user.getName()
                + " (" + user.getRole() + ", " + user.getLocation() + ").");
    }

    private void sendUsers() {
        if (repository.users().isEmpty()) {
            out.println("No users have been added yet.");
            return;
        }

        out.println("Users:");
        for (UserProfile user : repository.users()) {
            out.println("- " + user.getName() + " | " + user.getRole()
                    + " | " + user.getSkills() + " | " + user.getLocation());
        }
    }

    private void createJobPost(String text) {
        if (!canChangeData()) {
            return;
        }

        String[] parts = text.split("\\|", 4);

        if (parts.length < 4) {
            out.println("Usage: /post Job title | company | location | description");
            return;
        }

        JobPost job = repository.createJob(
                clean(parts[0]),
                clean(parts[1]),
                clean(parts[2]),
                clean(parts[3]),
                displayName
        );
        broadcastMessage("[JOB #" + job.getId() + "] " + job.getTitle() + " at "
                + job.getCompany() + " - " + job.getLocation() + " (posted by "
                + job.getPoster() + ")");
    }

    private void sendJobs() {
        List<JobPost> jobs = repository.jobs();
        if (jobs.isEmpty()) {
            out.println("No opportunities have been posted yet.");
            return;
        }

        out.println("Open opportunities:");
        for (JobPost job : jobs) {
            out.println("#" + job.getId() + " " + job.getTitle() + " at "
                    + job.getCompany() + " - " + job.getLocation());
            out.println("   " + job.getDescription());
            out.println("   Posted by " + job.getPoster() + ". Apply with /apply "
                    + job.getId() + " Your message");
        }
    }

    private void applyForJob(String text) {
        if (!canChangeData()) {
            return;
        }

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

        Optional<JobPost> job = repository.findJob(jobId);
        if (job.isEmpty()) {
            out.println("No job found with id #" + jobId + ".");
            return;
        }

        JobApplication application = repository.createApplication(jobId, displayName, parts[1]);
        broadcastMessage("[APPLICATION #" + application.getId() + "] " + displayName + " applied for #"
                + job.get().getId() + " " + job.get().getTitle() + " at " + job.get().getCompany()
                + ": " + parts[1]);
    }

    private void sendApplications(String jobIdText) {
        int jobId;
        try {
            jobId = Integer.parseInt(jobIdText.trim());
        } catch (NumberFormatException e) {
            out.println("Usage: /applications JobId");
            return;
        }

        List<JobApplication> applications = repository.applicationsForJob(jobId);
        if (applications.isEmpty()) {
            out.println("No applications found for job #" + jobId + ".");
            return;
        }

        out.println("Applications for job #" + jobId + ":");
        for (JobApplication application : applications) {
            out.println("#" + application.getId() + " " + application.getApplicant()
                    + ": " + application.getMessage());
        }
    }

    private void sendHelp() {
        out.println("Commands:");
        out.println("/register password");
        out.println("/login password");
        out.println("/profile Role | skills | location");
        out.println("/adduser Name | role | skills | location");
        out.println("/users");
        out.println("/post Job title | company | location | description");
        out.println("/jobs");
        out.println("/apply JobId Short application message");
        out.println("/applications JobId");
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

    private boolean canChangeData() {
        if (authenticated) {
            return true;
        }

        out.println("Please log in first with /login password.");
        return false;
    }
}
