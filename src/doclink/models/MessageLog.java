package doclink.models;

import java.time.LocalDateTime;

public class MessageLog {
    private int id;
    private LocalDateTime timestamp;
    private String recipient;
    private String messageType; // "EMAIL", "SMS", "IN_APP"
    private String subject; // For email/in-app, can be null for SMS
    private String message;
    private String status; // "Sent", "Failed", "Read", "Dismissed"
    private String details; // e.g., error message, API response

    public MessageLog(int id, LocalDateTime timestamp, String recipient, String messageType, String subject, String message, String status, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.recipient = recipient;
        this.messageType = messageType;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.details = details;
    }

    // Constructor for new logs (ID and timestamp will be generated)
    public MessageLog(String recipient, String messageType, String subject, String message, String status, String details) {
        this(0, LocalDateTime.now(), recipient, messageType, subject, message, status, details);
    }

    // Getters
    public int getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getRecipient() { return recipient; }
    public String getMessageType() { return messageType; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setMessage(String message) { this.message = message; }
    public void setStatus(String status) { this.status = status; }
    public void setDetails(String details) { this.details = details; }
}