package server.store;

public class JobMatch {
    private final JobPost job;
    private final int score;
    private final String reason;

    public JobMatch(JobPost job, int score, String reason) {
        this.job = job;
        this.score = score;
        this.reason = reason;
    }

    public JobPost getJob() {
        return job;
    }

    public int getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}
