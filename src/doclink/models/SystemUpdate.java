package doclink.models;

import java.time.LocalDateTime;

public class SystemUpdate {
    private int id;
    private String version;
    private String title;
    private String message;
    private LocalDateTime createdAt;

    public SystemUpdate(int id, String version, String title, String message, LocalDateTime createdAt) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }

    // Constructor for new updates (ID and createdAt will be generated)
    public SystemUpdate(String version, String title, String message) {
        this(0, version, title, message, LocalDateTime.now());
    }

    // Getters
    public int getId() { return id; }
    public String getVersion() { return version; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setVersion(String version) { this.version = version; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}