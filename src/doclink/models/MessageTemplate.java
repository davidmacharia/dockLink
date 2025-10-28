package doclink.models;

public class MessageTemplate {
    private int id;
    private String templateName;
    private String subject; // For email, can be null for SMS
    private String body;
    private String type; // "EMAIL", "SMS"

    public MessageTemplate(int id, String templateName, String subject, String body, String type) {
        this.id = id;
        this.templateName = templateName;
        this.subject = subject;
        this.body = body;
        this.type = type;
    }

    // Constructor for new templates (ID will be generated)
    public MessageTemplate(String templateName, String subject, String body, String type) {
        this(0, templateName, subject, body, type);
    }

    // Getters
    public int getId() { return id; }
    public String getTemplateName() { return templateName; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getType() { return type; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setBody(String body) { this.body = body; }
    public void setType(String type) { this.type = type; }
}