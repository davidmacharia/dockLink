package doclink.models;

public class UserPreference {

    private int id;
    private int userId;
    private boolean emailNotificationsEnabled;
    private boolean smsNotificationsEnabled;
    private int lastSeenUpdateId; // NEW: To track the last system update ID seen by the user

    public UserPreference(int id, int userId, boolean emailNotificationsEnabled, boolean smsNotificationsEnabled, int lastSeenUpdateId) {
        this.id = id;
        this.userId = userId;
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.smsNotificationsEnabled = smsNotificationsEnabled;
        this.lastSeenUpdateId = lastSeenUpdateId;
    }

    // Constructor for new preferences (ID will be generated)
    public UserPreference(int userId, boolean emailNotificationsEnabled, boolean smsNotificationsEnabled, int lastSeenUpdateId) {
        this(0, userId, emailNotificationsEnabled, smsNotificationsEnabled, lastSeenUpdateId);
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

    public int getLastSeenUpdateId() { // NEW Getter
        return lastSeenUpdateId;
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

    public void setLastSeenUpdateId(int lastSeenUpdateId) { // NEW Setter
        this.lastSeenUpdateId = lastSeenUpdateId;
    }
}