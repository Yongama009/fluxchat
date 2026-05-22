package server.store;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private String role = "No role yet";
    private String skills = "No skills yet";
    private String location = "No location yet";
    private String passwordHash = "";

    public UserProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getSkills() {
        return skills;
    }

    public String getLocation() {
        return location;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void update(String role, String skills, String location) {
        if (!role.isBlank()) {
            this.role = role;
        }

        if (!skills.isBlank()) {
            this.skills = skills;
        }

        if (!location.isBlank()) {
            this.location = location;
        }
    }

    public void updateFromProfileText(String profileText) {
        String[] parts = profileText.split("\\|", 3);

        String nextRole = parts.length > 0 ? parts[0].trim() : "";
        String nextSkills = parts.length > 1 ? parts[1].trim() : "";
        String nextLocation = parts.length > 2 ? parts[2].trim() : "";

        update(nextRole, nextSkills, nextLocation);
    }
}
