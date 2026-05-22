package server.store;

import java.io.Serializable;
import java.time.LocalDateTime;

public class JobApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final int jobId;
    private final String applicant;
    private final String message;
    private final LocalDateTime createdAt;

    public JobApplication(int id, int jobId, String applicant, String message) {
        this.id = id;
        this.jobId = jobId;
        this.applicant = applicant;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobId;
    }

    public String getApplicant() {
        return applicant;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
