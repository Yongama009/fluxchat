package server.store;

public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    public static String validate(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }

        if (!password.matches(".*[A-Z].*")) {
            return "Password must include at least one uppercase letter.";
        }

        if (!password.matches(".*[a-z].*")) {
            return "Password must include at least one lowercase letter.";
        }

        if (!password.matches(".*\\d.*")) {
            return "Password must include at least one digit.";
        }

        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Password must include at least one special character.";
        }

        return null;
    }
}
