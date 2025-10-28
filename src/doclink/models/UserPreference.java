package doclink.models;

public class UserPreference {

    private int id;
    private int userId;
    private boolean emailNotificationsEnabled;
    private boolean smsNotificationsEnabled;

    public UserPreference(int id, int userId, boolean emailNotificationsEnabled, boolean smsNotificationsEnabled) {
        this.id = id;
        this.userId = userId;
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.smsNotificationsEnabled = smsNotificationsEnabled;
    }

    // Constructor for new preferences (ID will be generated)
    public UserPreference(int userId, boolean emailNotificationsEnabled, boolean smsNotificationsEnabled) {
        this(0, userId, emailNotificationsEnabled, smsNotificationsEnabled);
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public boolean isSmsNotificationsEnabled() {
        return smsNotificationsEnabled;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public void setSmsNotificationsEnabled(boolean smsNotificationsEnabled) {
        this.smsNotificationsEnabled = smsNotificationsEnabled;
    }
}
