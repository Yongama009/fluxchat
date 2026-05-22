package server.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class StoredData implements Serializable {
    private static final long serialVersionUID = 1L;

    final Map<String, UserProfile> users;
    final List<JobPost> jobs;
    final List<JobApplication> applications;
    final int nextJobId;
    final int nextApplicationId;

    StoredData(Map<String, UserProfile> users,
               List<JobPost> jobs,
               List<JobApplication> applications,
               int nextJobId,
               int nextApplicationId) {
        this.users = new LinkedHashMap<>(users);
        this.jobs = new ArrayList<>(jobs);
        this.applications = new ArrayList<>(applications);
        this.nextJobId = nextJobId;
        this.nextApplicationId = nextApplicationId;
    }
}
