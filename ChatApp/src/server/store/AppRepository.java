package server.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AppRepository {
    private final Path dataFile;
    private final Map<String, UserProfile> users = new LinkedHashMap<>();
    private final List<JobPost> jobs = new ArrayList<>();
    private final List<JobApplication> applications = new ArrayList<>();
    private int nextJobId = 1;
    private int nextApplicationId = 1;

    public AppRepository(Path dataFile) {
        this.dataFile = dataFile;
        load();
    }

    public synchronized UserProfile getOrCreateUser(String name) {
        UserProfile user = users.computeIfAbsent(normalizeName(name), UserProfile::new);
        save();
        return user;
    }

    public synchronized Optional<UserProfile> findUser(String name) {
        return Optional.ofNullable(users.get(normalizeName(name)));
    }

    public synchronized Optional<UserProfile> findUserByIdNumber(String idNumber) {
        String normalizedId = idNumber.trim();
        return users.values().stream()
                .filter(user -> normalizedId.equals(user.getIdNumber()))
                .findFirst();
    }

    public synchronized boolean idNumberBelongsToAnotherUser(String idNumber, String name) {
        String normalizedName = normalizeName(name);
        return findUserByIdNumber(idNumber)
                .map(user -> !normalizeName(user.getName()).equals(normalizedName))
                .orElse(false);
    }

    public synchronized void saveUser(UserProfile user) {
        users.put(normalizeName(user.getName()), user);
        save();
    }

    public synchronized Collection<UserProfile> users() {
        return users.values().stream()
                .sorted(Comparator.comparing(UserProfile::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized JobPost createJob(String title,
                                          String company,
                                          String location,
                                          String description,
                                          String poster) {
        return createJob(title, company, location, description, poster, "");
    }

    public synchronized JobPost createJob(String title,
                                          String company,
                                          String location,
                                          String description,
                                          String poster,
                                          String sourceUrl) {
        JobPost job = new JobPost(nextJobId++, title, company, location, description, poster, sourceUrl);
        jobs.add(job);
        save();
        return job;
    }

    public synchronized List<JobPost> jobs() {
        return List.copyOf(jobs);
    }

    public synchronized Optional<JobPost> findJob(int jobId) {
        return jobs.stream().filter(job -> job.getId() == jobId).findFirst();
    }

    public synchronized List<JobMatch> matchingJobsFor(UserProfile user) {
        return jobs.stream()
                .map(job -> match(job, user))
                .filter(match -> match.getScore() > 0)
                .sorted(Comparator.comparingInt(JobMatch::getScore).reversed())
                .toList();
    }

    public synchronized JobApplication createApplication(int jobId, String applicant, String message) {
        JobApplication application =
                new JobApplication(nextApplicationId++, jobId, applicant, message);
        applications.add(application);
        save();
        return application;
    }

    public synchronized List<JobApplication> applicationsForJob(int jobId) {
        return applications.stream()
                .filter(application -> application.getJobId() == jobId)
                .toList();
    }

    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(dataFile)))) {
            StoredData data = (StoredData) in.readObject();
            users.clear();
            users.putAll(data.users);
            jobs.clear();
            jobs.addAll(data.jobs);
            applications.clear();
            applications.addAll(data.applications);
            nextJobId = Math.max(data.nextJobId, nextIdFromJobs());
            nextApplicationId = Math.max(data.nextApplicationId, nextIdFromApplications());
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Could not load data from " + dataFile, e);
        }
    }

    private void save() {
        try {
            Path parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path tempFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            StoredData data = new StoredData(
                    new LinkedHashMap<>(users),
                    new ArrayList<>(jobs),
                    new ArrayList<>(applications),
                    nextJobId,
                    nextApplicationId
            );

            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                out.writeObject(data);
            }

            Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save data to " + dataFile, e);
        }
    }

    private int nextIdFromJobs() {
        return jobs.stream().mapToInt(JobPost::getId).max().orElse(0) + 1;
    }

    private int nextIdFromApplications() {
        return applications.stream().mapToInt(JobApplication::getId).max().orElse(0) + 1;
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private JobMatch match(JobPost job, UserProfile user) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String searchableJobText = (job.getTitle() + " " + job.getDescription()).toLowerCase();

        Set<String> skillTokens = tokens(user.getSkills());
        long skillHits = skillTokens.stream()
                .filter(searchableJobText::contains)
                .count();
        if (skillHits > 0) {
            score += (int) skillHits * 20;
            reasons.add(skillHits + " skill match" + (skillHits == 1 ? "" : "es"));
        }

        Set<String> roleTokens = tokens(user.getRole());
        long roleHits = roleTokens.stream()
                .filter(searchableJobText::contains)
                .count();
        if (roleHits > 0) {
            score += (int) roleHits * 15;
            reasons.add("role match");
        }

        String userLocation = user.getLocation().toLowerCase();
        String jobLocation = job.getLocation().toLowerCase();
        if (!userLocation.isBlank()
                && (jobLocation.contains(userLocation) || jobLocation.contains("remote"))) {
            score += 10;
            reasons.add("location match");
        }

        return new JobMatch(job, score, String.join(", ", reasons));
    }

    private Set<String> tokens(String text) {
        return List.of(text.toLowerCase().split("[^a-z0-9+#]+")).stream()
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }
}
