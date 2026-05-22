package server.store;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private String firstName = "";
    private String lastName = "";
    private String idNumber = "";
    private String email = "";
    private String phone = "";
    private String role = "No role yet";
    private String skills = "No skills yet";
    private String location = "No location yet";
    private String education = "";
    private String experience = "";
    private boolean identityValidated;
    private String passwordHash = "";
    private int passwordPolicyVersion;

    public UserProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
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

    public String getEducation() {
        return education;
    }

    public String getExperience() {
        return experience;
    }

    public boolean isIdentityValidated() {
        return identityValidated;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordPolicyVersion = PasswordPolicy.CURRENT_VERSION;
    }

    public int getPasswordPolicyVersion() {
        return passwordPolicyVersion;
    }

    public boolean requiresPasswordUpgrade() {
        return hasPassword() && passwordPolicyVersion < PasswordPolicy.CURRENT_VERSION;
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

    public void completeCvProfile(String firstName,
                                  String lastName,
                                  String idNumber,
                                  String email,
                                  String phone,
                                  String location,
                                  String role,
                                  String skills,
                                  String education,
                                  String experience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.idNumber = idNumber;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.role = role;
        this.skills = skills;
        this.education = education;
        this.experience = experience;
        this.identityValidated = true;
    }

    public boolean hasCompletedCvProfile() {
        return identityValidated
                && !firstName.isBlank()
                && !lastName.isBlank()
                && !idNumber.isBlank()
                && !email.isBlank()
                && !phone.isBlank()
                && !location.isBlank()
                && !role.isBlank()
                && !skills.isBlank()
                && !education.isBlank()
                && !experience.isBlank();
    }

    public String cvSummary() {
        return firstName + " " + lastName + " | " + idNumber + " | " + email
                + " | " + phone + " | " + role + " | " + skills + " | "
                + location + " | Education: " + education + " | Experience: "
                + experience;
    }
}
