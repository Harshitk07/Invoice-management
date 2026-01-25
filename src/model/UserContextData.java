package model;

public final class UserContextData {

    private final String username;
    private final String role;

    public UserContextData(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}

