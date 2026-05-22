package server;

import server.store.AppRepository;
import server.store.JobApplication;
import server.store.JobMatch;
import server.store.JobPost;
import server.store.JobSafetyCheck;
import server.store.PasswordHasher;
import server.store.PasswordPolicy;
import server.store.SaIdValidator;
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
    private boolean passwordUpgradeRequired;

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
            if (authenticated) {
                broadcastSystemMessage(displayName + " joined the opportunity network.");
            }

            String message;
            while ((message = in.readLine()) != null) {
                handleMessage(message.trim());
            }
        } catch (IOException e) {
            System.out.println(displayName + " disconnected.");
        } finally {
            clients.remove(this);
            closeSocket();
            if (authenticated) {
                broadcastSystemMessage(displayName + " left the opportunity network.");
            }
        }
    }

    private void askForDisplayName() throws IOException {
        out.println("Enter your name:");
        String name = in.readLine();

        if (name != null && !name.trim().isEmpty()) {
            displayName = clean(name);
        }

        repository.findUser(displayName)
                .ifPresent(user -> authenticated = !user.hasPassword());
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
        } else if (message.toLowerCase().startsWith("/registercv ")) {
            registerCv(message.substring("/registercv ".length()).trim());
        } else if (message.toLowerCase().startsWith("/loginid ")) {
            loginById(message.substring("/loginid ".length()).trim());
        } else if (message.toLowerCase().startsWith("/changepassword ")) {
            changePassword(message.substring("/changepassword ".length()).trim());
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
        } else if (message.equalsIgnoreCase("/matches")) {
            sendMatches();
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
        String passwordError = PasswordPolicy.validate(password);
        if (passwordError != null) {
            out.println(passwordError);
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
        passwordUpgradeRequired = false;
        out.println("Account registered for " + displayName + ".");
    }

    private void registerCv(String text) {
        String[] parts = text.split("\\|", 11);
        if (parts.length < 11) {
            out.println("Usage: /registercv First name | Last name | ID number | email | phone | location | role | skills | education | experience | password");
            return;
        }

        String firstName = clean(parts[0]);
        String lastName = clean(parts[1]);
        String idNumber = parts[2].trim();
        String email = parts[3].trim().toLowerCase();
        String phone = clean(parts[4]);
        String location = clean(parts[5]);
        String role = clean(parts[6]);
        String skills = clean(parts[7]);
        String education = clean(parts[8]);
        String experience = clean(parts[9]);
        String password = parts[10].trim();

        String validationError = validateRegistration(firstName, lastName, idNumber, email,
                phone, location, role, skills, education, experience, password);
        if (validationError != null) {
            out.println(validationError);
            return;
        }

        String accountName = firstName + " " + lastName;
        if (repository.idNumberBelongsToAnotherUser(idNumber, accountName)) {
            out.println("That ID number is already registered.");
            return;
        }

        UserProfile user = repository.getOrCreateUser(accountName);
        if (user.hasPassword()) {
            out.println("An account already exists for " + accountName + ". Use Login.");
            return;
        }

        user.completeCvProfile(firstName, lastName, idNumber, email, phone, location,
                role, skills, education, experience);
        user.setPasswordHash(PasswordHasher.hash(password));
        repository.saveUser(user);

        displayName = accountName;
        authenticated = true;
        passwordUpgradeRequired = false;
        out.println("Account registered and CV profile completed for " + displayName + ".");
        out.println("Identity check: SA ID format, birth date, and checksum passed.");
        sendMatchesFor(user);
        broadcastSystemMessage(displayName + " joined with a completed CV profile.");
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
        passwordUpgradeRequired = user.get().requiresPasswordUpgrade();
        out.println("Logged in as " + displayName + ".");
        if (passwordUpgradeRequired) {
            out.println("PASSWORD_UPGRADE_REQUIRED: Your password no longer meets the current security rules. Use /changepassword oldPassword newStrongPassword before continuing.");
        }
    }

    private void loginById(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            out.println("Usage: /loginid IDNumber password");
            return;
        }

        Optional<UserProfile> user = repository.findUserByIdNumber(parts[0].trim());
        if (user.isEmpty() || !user.get().hasPassword()) {
            out.println("No registered account found for that ID number.");
            return;
        }

        if (!PasswordHasher.verify(parts[1].trim(), user.get().getPasswordHash())) {
            out.println("Login failed.");
            return;
        }

        displayName = user.get().getName();
        authenticated = true;
        passwordUpgradeRequired = user.get().requiresPasswordUpgrade();
        out.println("Logged in as " + displayName + ".");
        if (passwordUpgradeRequired) {
            out.println("PASSWORD_UPGRADE_REQUIRED: Your password no longer meets the current security rules. Use /changepassword oldPassword newStrongPassword before continuing.");
            return;
        }
        if (user.get().hasCompletedCvProfile()) {
            out.println("CV profile is complete. You can apply for jobs.");
            sendMatchesFor(user.get());
        }
        broadcastSystemMessage(displayName + " logged in.");
    }

    private void changePassword(String text) {
        if (!authenticated) {
            out.println("Please log in first.");
            return;
        }

        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            out.println("Usage: /changepassword oldPassword newStrongPassword");
            return;
        }

        Optional<UserProfile> user = repository.findUser(displayName);
        if (user.isEmpty() || !PasswordHasher.verify(parts[0], user.get().getPasswordHash())) {
            out.println("Current password is incorrect.");
            return;
        }

        String passwordError = PasswordPolicy.validate(parts[1].trim());
        if (passwordError != null) {
            out.println(passwordError);
            return;
        }

        user.get().setPasswordHash(PasswordHasher.hash(parts[1].trim()));
        repository.saveUser(user.get());
        passwordUpgradeRequired = false;
        out.println("Password updated. Account access restored.");
        if (user.get().hasCompletedCvProfile()) {
            sendMatchesFor(user.get());
        }
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
                    + " | " + user.getSkills() + " | " + user.getLocation()
                    + " | CV " + (user.hasCompletedCvProfile() ? "complete" : "incomplete"));
        }
    }

    private void createJobPost(String text) {
        if (!canChangeData()) {
            return;
        }

        String[] parts = text.split("\\|", 5);

        if (parts.length < 4) {
            out.println("Usage: /post Job title | company | location | description | optional source URL");
            return;
        }

        String sourceUrl = parts.length > 4 ? clean(parts[4]) : "";
        List<String> safetyProblems = JobSafetyCheck.validate(
                clean(parts[0]),
                clean(parts[1]),
                clean(parts[3]),
                sourceUrl
        );
        if (!safetyProblems.isEmpty()) {
            out.println("Job rejected for safety review:");
            for (String problem : safetyProblems) {
                out.println("- " + problem);
            }
            return;
        }

        JobPost job = repository.createJob(
                clean(parts[0]),
                clean(parts[1]),
                clean(parts[2]),
                clean(parts[3]),
                displayName,
                sourceUrl
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
            if (!job.getSourceUrl().isBlank()) {
                out.println("   Source: " + job.getSourceUrl());
            }
            out.println("   Posted by " + job.getPoster() + ". Apply with /apply "
                    + job.getId() + " Your message");
        }
    }

    private void sendMatches() {
        if (!canChangeData()) {
            return;
        }

        Optional<UserProfile> user = repository.findUser(displayName);
        if (user.isEmpty() || !user.get().hasCompletedCvProfile()) {
            out.println("Complete registration with a CV profile before viewing matches.");
            return;
        }

        sendMatchesFor(user.get());
    }

    private void sendMatchesFor(UserProfile user) {
        List<JobMatch> matches = repository.matchingJobsFor(user);
        if (matches.isEmpty()) {
            out.println("Matched opportunities: none yet. Use Jobs to view all available posts.");
            return;
        }

        out.println("Matched opportunities for " + user.getRole() + ":");
        for (JobMatch match : matches) {
            JobPost job = match.getJob();
            out.println("#" + job.getId() + " " + job.getTitle() + " at "
                    + job.getCompany() + " - " + job.getLocation()
                    + " [score " + match.getScore() + ": " + match.getReason() + "]");
            out.println("   " + job.getDescription());
            if (!job.getSourceUrl().isBlank()) {
                out.println("   Source: " + job.getSourceUrl());
            }
        }
    }

    private void applyForJob(String text) {
        if (!canChangeData()) {
            return;
        }

        Optional<UserProfile> applicant = repository.findUser(displayName);
        if (applicant.isEmpty() || !applicant.get().hasCompletedCvProfile()) {
            out.println("Complete registration with a validated CV profile before applying.");
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

        JobApplication application = repository.createApplication(jobId, displayName,
                parts[1] + " | CV: " + applicant.get().cvSummary());
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
        out.println("/registercv First name | Last name | ID number | email | phone | location | role | skills | education | experience | password");
        out.println("/loginid IDNumber password");
        out.println("/changepassword oldPassword newStrongPassword");
        out.println("/register password");
        out.println("/login password");
        out.println("/profile Role | skills | location");
        out.println("/adduser Name | role | skills | location");
        out.println("/users");
        out.println("/post Job title | company | location | description");
        out.println("/jobs");
        out.println("/matches");
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
            if (passwordUpgradeRequired) {
                out.println("Password update required before continuing. Use /changepassword oldPassword newStrongPassword.");
                return false;
            }
            return true;
        }

        out.println("Please log in first with /loginid IDNumber password.");
        return false;
    }

    private String validateRegistration(String firstName,
                                        String lastName,
                                        String idNumber,
                                        String email,
                                        String phone,
                                        String location,
                                        String role,
                                        String skills,
                                        String education,
                                        String experience,
                                        String password) {
        if (!isValidName(firstName) || !isValidName(lastName)) {
            return "First name and last name must contain letters only and be at least 2 characters.";
        }

        if (!SaIdValidator.isValid(idNumber)) {
            return "ID number is not valid. Use a 13-digit South African ID with a valid date and checksum.";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Email address is not valid.";
        }

        if (!phone.matches("^[+0-9][0-9\\s-]{8,18}$")) {
            return "Phone number is not valid.";
        }

        if (location.isBlank() || role.isBlank() || skills.isBlank()
                || education.isBlank() || experience.isBlank()) {
            return "Location, role, skills, education, and experience are required.";
        }

        String passwordError = PasswordPolicy.validate(password);
        if (passwordError != null) {
            return passwordError;
        }

        return null;
    }

    private boolean isValidName(String value) {
        return value.matches("[A-Za-z][A-Za-z '-]{1,49}");
    }
}
