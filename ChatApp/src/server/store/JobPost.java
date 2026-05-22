package server.store;

import java.io.Serializable;
import java.time.LocalDateTime;

public class JobPost implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String title;
    private final String company;
    private final String location;
    private final String description;
    private final String poster;
    private final String sourceUrl;
    private final LocalDateTime createdAt;

    public JobPost(int id,
                   String title,
                   String company,
                   String location,
                   String description,
                   String poster) {
        this(id, title, company, location, description, poster, "");
    }

    public JobPost(int id,
                   String title,
                   String company,
                   String location,
                   String description,
                   String poster,
                   String sourceUrl) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.poster = poster;
        this.sourceUrl = sourceUrl;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getPoster() {
        return poster;
    }

    public String getSourceUrl() {
        return sourceUrl == null ? "" : sourceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
