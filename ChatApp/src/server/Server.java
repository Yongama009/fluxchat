package server;

import server.store.AppRepository;
import server.store.JobPost;
import server.store.PasswordHasher;
import server.store.PasswordPolicy;
import server.store.SaIdValidator;
import server.store.UserProfile;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Start a small HTTP server on (port + 1) to serve the web UI and a few API endpoints
    private static void startHttpServer(AppRepository repository, int port) {
        try {
            int httpPort = port + 1;
            HttpServer http = HttpServer.create(new InetSocketAddress(httpPort), 0);
            Path webRoot = Path.of("ChatApp", "web");

            // Serve static files (index.html and assets)
            http.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.equals("")) {
                    path = "/index.html";
                }
                Path file = webRoot.resolve(path.substring(1)).normalize();
                if (!file.startsWith(webRoot) || !Files.exists(file)) {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }
                byte[] data = Files.readAllBytes(file);
                String contentType = guessContentType(file.toString());
                exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            });

            // API: POST /api/registercv
            http.createContext("/api/registercv", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String body = readAll(exchange.getRequestBody());
                    Map<String, String> m = parseJson(body);
                    String[] required = {"firstName","lastName","idNumber","email","phone","location","role","skills","education","experience","password"};
                    for (String k : required) {
                        if (!m.containsKey(k) || m.get(k).isBlank()) {
                            sendJson(exchange, 400, Map.of("error", "Please provide: " + k));
                            return;
                        }
                    }

                    String firstName = m.get("firstName").trim();
                    String lastName = m.get("lastName").trim();
                    String idNumber = m.get("idNumber").trim();
                    String email = m.get("email").trim().toLowerCase();
                    String phone = m.get("phone").trim();
                    String location = m.get("location").trim();
                    String role = m.get("role").trim();
                    String skills = m.get("skills").trim();
                    String education = m.get("education").trim();
                    String experience = m.get("experience").trim();
                    String password = m.get("password");

                    // basic validation similar to console flow
                    if (!firstName.matches("[A-Za-z][A-Za-z '-]{1,49}") || !lastName.matches("[A-Za-z][A-Za-z '-]{1,49}")) {
                        sendJson(exchange, 400, Map.of("error", "Name must start with a letter and contain only letters, spaces, hyphens, or apostrophes (2-50 chars)"));
                        return;
                    }
                    if (!SaIdValidator.isValid(idNumber)) {
                        sendJson(exchange, 400, Map.of("error", "South African ID number format is invalid. Format: YYMMDDGGGGGCAZA (13 digits)"));
                        return;
                    }
                    if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                        sendJson(exchange, 400, Map.of("error", "Email address format is invalid. Example: user@example.com"));
                        return;
                    }
                    if (!phone.matches("^[+0-9][0-9\\s-]{8,18}$")) {
                        sendJson(exchange, 400, Map.of("error", "Phone number format is invalid. Example: 0712345678 or +27712345678"));
                        return;
                    }
                    String pwdErr = PasswordPolicy.validate(password);
                    if (pwdErr != null) {
                        sendJson(exchange, 400, Map.of("error", pwdErr));
                        return;
                    }

                    String accountName = firstName + " " + lastName;
                    if (repository.idNumberBelongsToAnotherUser(idNumber, accountName)) {
                        sendJson(exchange, 409, Map.of("error", "This ID number is already registered to another account"));
                        return;
                    }

                    UserProfile user = repository.getOrCreateUser(accountName);
                    if (user.hasPassword()) {
                        sendJson(exchange, 409, Map.of("error", "An account already exists for " + accountName + ". Please sign in instead."));
                        return;
                    }

                    user.completeCvProfile(firstName, lastName, idNumber, email, phone, location, role, skills, education, experience);
                    user.setPasswordHash(PasswordHasher.hash(password));
                    repository.saveUser(user);

                    sendJson(exchange, 200, Map.of("message", "✓ Account created successfully! You can now sign in.", "token", user.getName()));
                }
            });

            // API: POST /api/login
            http.createContext("/api/login", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String body = readAll(exchange.getRequestBody());
                    Map<String, String> m = parseJson(body);
                    String idNumber = m.getOrDefault("idNumber", "").trim();
                    String password = m.getOrDefault("password", "");
                    if (idNumber.isBlank() || password.isEmpty()) {
                        sendJson(exchange, 400, Map.of("error", "Please provide both ID number and password"));
                        return;
                    }
                    var userOpt = repository.findUserByIdNumber(idNumber);
                    if (userOpt.isEmpty() || !userOpt.get().hasPassword()) {
                        sendJson(exchange, 401, Map.of("error", "ID number or password is incorrect"));
                        return;
                    }
                    UserProfile user = userOpt.get();
                    if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                        sendJson(exchange, 401, Map.of("error", "ID number or password is incorrect"));
                        return;
                    }
                    sendJson(exchange, 200, Map.of("message", "✓ Welcome back! Signed in successfully", "token", user.getName()));
                }
            });

            // API: GET /api/jobs
            http.createContext("/api/jobs", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    List<JobPost> jobs = repository.jobs();
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (JobPost j : jobs) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", j.getId());
                        item.put("title", j.getTitle());
                        item.put("company", j.getCompany());
                        item.put("location", j.getLocation());
                        item.put("description", j.getDescription());
                        item.put("sourceUrl", j.getSourceUrl());
                        item.put("poster", j.getPoster());
                        out.add(item);
                    }
                    sendJson(exchange, 200, Map.of("jobs", out));
                }
            });

            // API: POST /api/post (create job)
            http.createContext("/api/post", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String body = readAll(exchange.getRequestBody());
                    Map<String, String> m = parseJson(body);
                    String token = m.getOrDefault("token", "").trim();
                    if (token.isBlank()) {
                        sendJson(exchange, 401, Map.of("error", "You must be signed in to post a job"));
                        return;
                    }
                    var userOpt = repository.findUser(token);
                    if (userOpt.isEmpty()) {
                        sendJson(exchange, 401, Map.of("error", "Session expired. Please sign in again"));
                        return;
                    }
                    String title = m.getOrDefault("title", "").trim();
                    String company = m.getOrDefault("company", "").trim();
                    String location = m.getOrDefault("location", "").trim();
                    String description = m.getOrDefault("description", "").trim();
                    String sourceUrl = m.getOrDefault("sourceUrl", "").trim();
                    if (title.isBlank() || company.isBlank() || location.isBlank() || description.isBlank()) {
                        sendJson(exchange, 400, Map.of("error", "Job title, company, location, and description are required"));
                        return;
                    }
                    JobPost job = repository.createJob(title, company, location, description, token, sourceUrl);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("id", job.getId());
                    resp.put("message", "✓ Job posted successfully!");
                    sendJson(exchange, 201, resp);
                }
            });

            // API: POST /api/apply (apply for job)
            http.createContext("/api/apply", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String body = readAll(exchange.getRequestBody());
                    Map<String, String> m = parseJson(body);
                    String token = m.getOrDefault("token", "").trim();
                    String jobIdStr = m.getOrDefault("jobId", "").trim();
                    String message = m.getOrDefault("message", "").trim();
                    if (token.isBlank() || jobIdStr.isBlank() || message.isBlank()) {
                        sendJson(exchange, 400, Map.of("error", "Please provide your message and ensure you are signed in"));
                        return;
                    }
                    int jobId;
                    try {
                        jobId = Integer.parseInt(jobIdStr);
                    } catch (NumberFormatException e) {
                        sendJson(exchange, 400, Map.of("error", "Invalid job ID"));
                        return;
                    }
                    var userOpt = repository.findUser(token);
                    if (userOpt.isEmpty() || !userOpt.get().hasCompletedCvProfile()) {
                        sendJson(exchange, 401, Map.of("error", "Your CV profile must be complete to apply for jobs"));
                        return;
                    }
                    var jobOpt = repository.findJob(jobId);
                    if (jobOpt.isEmpty()) {
                        sendJson(exchange, 404, Map.of("error", "Job posting not found"));
                        return;
                    }
                    String fullMessage = message + " | CV: " + userOpt.get().cvSummary();
                    var app = repository.createApplication(jobId, token, fullMessage);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("id", app.getId());
                    resp.put("message", "✓ Application submitted successfully!");
                    sendJson(exchange, 201, resp);
                }
            });

            // API: GET /api/matches (job matching for logged-in user)
            http.createContext("/api/matches", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> params = new HashMap<>();
                    if (query != null) {
                        for (String part : query.split("&")) {
                            String[] kv = part.split("=", 2);
                            if (kv.length == 2) {
                                params.put(kv[0], kv[1]);
                            }
                        }
                    }
                    String token = params.getOrDefault("token", "").trim();
                    if (token.isBlank()) {
                        sendJson(exchange, 401, Map.of("error", "You must be signed in to view job matches"));
                        return;
                    }
                    var userOpt = repository.findUser(token);
                    if (userOpt.isEmpty() || !userOpt.get().hasCompletedCvProfile()) {
                        sendJson(exchange, 401, Map.of("error", "Your CV profile needs to be complete to see personalized job matches"));
                        return;
                    }
                    var matches = repository.matchingJobsFor(userOpt.get());
                    List<Map<String, Object>> out = new ArrayList<>();
                    for (var match : matches) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", match.getJob().getId());
                        item.put("title", match.getJob().getTitle());
                        item.put("company", match.getJob().getCompany());
                        item.put("location", match.getJob().getLocation());
                        item.put("description", match.getJob().getDescription());
                        item.put("score", match.getScore());
                        item.put("reason", match.getReason());
                        out.add(item);
                    }
                    sendJson(exchange, 200, Map.of("matches", out));
                }
            });

            // API: GET /api/profile (get user profile)
            http.createContext("/api/profile", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String query = exchange.getRequestURI().getQuery();
                    String token = "";
                    if (query != null) {
                        for (String part : query.split("&")) {
                            String[] kv = part.split("=", 2);
                            if ("token".equals(kv[0]) && kv.length == 2) {
                                token =kv[1].trim();
                                break;
                            }
                        }
                    }
                    if (token.isBlank()) {
                        sendJson(exchange, 401, Map.of("error", "You must be signed in to view your profile"));
                        return;
                    }
                    var userOpt = repository.findUser(token);
                    if (userOpt.isEmpty()) {
                        sendJson(exchange, 404, Map.of("error", "Session expired. Please sign in again"));
                        return;
                    }
                    UserProfile user = userOpt.get();
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("name", user.getName());
                    resp.put("role", user.getRole());
                    resp.put("skills", user.getSkills());
                    resp.put("location", user.getLocation());
                    resp.put("idNumber", user.getIdNumber());
                    resp.put("email", user.getEmail());
                    resp.put("phone", user.getPhone());
                    resp.put("education", user.getEducation());
                    resp.put("experience", user.getExperience());
                    resp.put("cvComplete", user.hasCompletedCvProfile());
                    sendJson(exchange, 200, resp);
                }
            });

            // API: POST /api/updateprofile (update user profile)
            http.createContext("/api/updateprofile", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                        return;
                    }
                    String body = readAll(exchange.getRequestBody());
                    Map<String, String> m = parseJson(body);
                    String token = m.getOrDefault("token", "").trim();
                    if (token.isBlank()) {
                        sendJson(exchange, 401, Map.of("error", "Unauthorized - token required"));
                        return;
                    }
                    var userOpt = repository.findUser(token);
                    if (userOpt.isEmpty()) {
                        sendJson(exchange, 404, Map.of("error", "User not found"));
                        return;
                    }
                    
                    String email = m.getOrDefault("email", "").trim();
                    String phone = m.getOrDefault("phone", "").trim();
                    String location = m.getOrDefault("location", "").trim();
                    String role = m.getOrDefault("role", "").trim();
                    String skills = m.getOrDefault("skills", "").trim();
                    String education = m.getOrDefault("education", "").trim();
                    String experience = m.getOrDefault("experience", "").trim();
                    
                    // Validate email if provided
                    if (!email.isBlank() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                        sendJson(exchange, 400, Map.of("error", "Email address is not valid"));
                        return;
                    }
                    
                    // Validate phone if provided
                    if (!phone.isBlank() && !phone.matches("^[+0-9][0-9\\s-]{8,18}$")) {
                        sendJson(exchange, 400, Map.of("error", "Phone number is not valid"));
                        return;
                    }
                    
                    UserProfile user = userOpt.get();
                    user.updateProfile(email, phone, location, role, skills, education, experience);
                    repository.saveUser(user);
                    
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("message", "Profile updated successfully");
                    resp.put("name", user.getName());
                    resp.put("role", user.getRole());
                    resp.put("skills", user.getSkills());
                    resp.put("location", user.getLocation());
                    resp.put("email", user.getEmail());
                    resp.put("phone", user.getPhone());
                    resp.put("education", user.getEducation());
                    resp.put("experience", user.getExperience());
                    sendJson(exchange, 200, resp);
                }
            });

            http.setExecutor(null);
            http.start();
            System.out.println("HTTP UI started on port " + http.getAddress().getPort() + " (serve web/index.html)");
        } catch (IOException e) {
            System.out.println("Failed to start HTTP server: " + e.getMessage());
        }
    }

    private static String guessContentType(String filename) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
    }

    private static void sendJson(HttpExchange exchange, int status, Map<String, ?> data) throws IOException {
        String json = toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    // Very small naive JSON serializer for our simple maps/lists (no escaping for simplicity)
    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (var e : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':');
                sb.append(toJson(e.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (var v : (List<?>) obj) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(v));
            }
            sb.append(']');
            return sb.toString();
        }
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        // treat as string
        return '"' + escape(String.valueOf(obj)) + '"';
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // Very small JSON parser supporting flat string values: {"key":"value",...}
    private static Map<String, String> parseJson(String s) {
        Map<String, String> map = new HashMap<>();
        int i = 0, n = s.length();
        while (i < n) {
            int k1 = s.indexOf('"', i);
            if (k1 < 0) break;
            int k2 = s.indexOf('"', k1 + 1);
            if (k2 < 0) break;
            String key = s.substring(k1 + 1, k2);
            int colon = s.indexOf(':', k2);
            if (colon < 0) break;
            int vStart = s.indexOf('"', colon);
            if (vStart < 0) break;
            int vEnd = s.indexOf('"', vStart + 1);
            if (vEnd < 0) break;
            String val = s.substring(vStart + 1, vEnd);
            map.put(key, val);
            i = vEnd + 1;
        }
        return map;
    }

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

        // start the HTTP UI server (serves ChatApp/web and simple API) on port+1
        startHttpServer(repository, port);

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
