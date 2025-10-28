package doclink.communication;

import doclink.AppConfig;
import doclink.Database;
import doclink.models.MessageLog;
import doclink.models.MessageTemplate;
import doclink.models.User;
import doclink.models.UserPreference;
import doclink.models.SystemUpdate; // Added import for SystemUpdate

import jakarta.mail.*; // Changed from javax.mail.*
import jakarta.mail.internet.InternetAddress; // Changed from javax.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage; // Changed from javax.mail.internet.MimeMessage
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunicationManager {

    private Consumer<String> logConsumer; // For logging messages to the UI

    public CommunicationManager(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
        System.out.println("[CommunicationManager] " + message);
    }

    private boolean areAutoNotificationsEnabled() {
        return AppConfig.getBooleanProperty(AppConfig.AUTO_NOTIFICATIONS_ENABLED_KEY, false);
    }

    // --- Core Sending Methods (Placeholders for actual external API calls) ---

    /**
     * Sends an email. Requires JavaMail API dependency.
     *
     * @param recipient The email address of the recipient.
     * @param subject The subject of the email.
     * @param body The body of the email.
     * @return true if email was "sent" (simulated), false otherwise.
     */
    public boolean sendEmail(String recipient, String subject, String body) {
        if (!areAutoNotificationsEnabled()) {
            log("Email notifications are disabled in settings. Not sending to " + recipient);
            Database.addMessageLog(new MessageLog(recipient, "EMAIL", subject, body, "Skipped (Disabled)", "Auto-notifications disabled."));
            return false;
        }

        String smtpHost = AppConfig.getProperty(AppConfig.SMTP_HOST_KEY);
        int smtpPort = AppConfig.getIntProperty(AppConfig.SMTP_PORT_KEY, 587);
        String smtpUsername = AppConfig.getProperty(AppConfig.SMTP_USERNAME_KEY);
        String smtpPassword = AppConfig.getProperty(AppConfig.SMTP_PASSWORD_KEY);
        String senderEmail = AppConfig.getProperty(AppConfig.SENDER_EMAIL_KEY);

        if (smtpHost == null || smtpHost.isEmpty() || smtpUsername == null || smtpUsername.isEmpty() ||
            smtpPassword == null || smtpPassword.isEmpty() || senderEmail == null || senderEmail.isEmpty()) {
            log("Email configuration incomplete. Cannot send email to " + recipient);
            Database.addMessageLog(new MessageLog(recipient, "EMAIL", subject, body, "Failed", "Email configuration incomplete."));
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            log("Email sent successfully to: " + recipient);
            Database.addMessageLog(new MessageLog(recipient, "EMAIL", subject, body, "Sent", "Successfully sent."));
            return true;
        } catch (MessagingException e) {
            log("Failed to send email to " + recipient + ": " + e.getMessage());
            Database.addMessageLog(new MessageLog(recipient, "EMAIL", subject, body, "Failed", e.getMessage()));
            return false;
        }
    }

    /**
     * Sends an SMS message. Requires integration with an SMS Gateway API.
     *
     * @param phoneNumber The recipient's phone number.
     * @param message The message body.
     * @return true if SMS was "sent" (simulated), false otherwise.
     */
    public boolean sendSMS(String phoneNumber, String message) {
        if (!areAutoNotificationsEnabled()) {
            log("SMS notifications are disabled in settings. Not sending to " + phoneNumber);
            Database.addMessageLog(new MessageLog(phoneNumber, "SMS", null, message, "Skipped (Disabled)", "Auto-notifications disabled."));
            return false;
        }

        String smsApiKey = AppConfig.getProperty(AppConfig.SMS_API_KEY);
        String smsSenderId = AppConfig.getProperty(AppConfig.SMS_SENDER_ID_KEY);

        if (smsApiKey == null || smsApiKey.isEmpty() || smsSenderId == null || smsSenderId.isEmpty()) {
            log("SMS configuration incomplete. Cannot send SMS to " + phoneNumber);
            Database.addMessageLog(new MessageLog(phoneNumber, "SMS", null, message, "Failed", "SMS configuration incomplete."));
            return false;
        }

        log("Simulating SMS to " + phoneNumber + ": " + message);
        // In a real application, this would involve an HTTP POST request to an SMS gateway API
        // e.g., Twilio, Nexmo, etc.
        // Example:
        // try {
        //     URL url = new URL("https://api.sms-gateway.com/send");
        //     HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //     conn.setRequestMethod("POST");
        //     conn.setRequestProperty("Content-Type", "application/json");
        //     conn.setDoOutput(true);
        //     String jsonInputString = String.format("{\"apiKey\": \"%s\", \"senderId\": \"%s\", \"to\": \"%s\", \"message\": \"%s\"}", smsApiKey, smsSenderId, phoneNumber, message);
        //     try (OutputStream os = conn.getOutputStream()) {
        //         byte[] input = jsonInputString.getBytes("utf-8");
        //         os.write(input, 0, input.length);
        //     }
        //     int responseCode = conn.getResponseCode();
        //     if (responseCode == HttpURLConnection.HTTP_OK) {
        //         log("SMS sent successfully to: " + phoneNumber);
        //         Database.addMessageLog(new MessageLog(phoneNumber, "SMS", null, message, "Sent", "Successfully sent."));
        //         return true;
        //     } else {
        //         log("SMS failed to send to " + phoneNumber + ". Response code: " + responseCode);
        //         Database.addMessageLog(new MessageLog(phoneNumber, "SMS", null, message, "Failed", "API response code: " + responseCode));
        //         return false;
        //     }
        // } catch (IOException e) {
        //     log("Error sending SMS to " + phoneNumber + ": " + e.getMessage());
        //     Database.addMessageLog(new MessageLog(phoneNumber, "SMS", null, message, "Failed", e.getMessage()));
        //     return false;
        // }
        Database.addMessageLog(new MessageLog(phoneNumber, "SMS", null, message, "Sent (Simulated)", "Simulated successful send."));
        return true;
    }

    /**
     * Replaces placeholders in a message template body/subject with actual values.
     * Placeholders are in the format {key}.
     *
     * @param templateContent The template string with placeholders.
     * @param replacements A list of key-value pairs for replacements.
     * @return The processed string.
     */
    public String replacePlaceholders(String templateContent, List<String[]> replacements) {
        String result = templateContent;
        for (String[] replacement : replacements) {
            if (replacement.length == 2) {
                result = result.replace("{" + replacement[0] + "}", replacement[1]);
            }
        }
        // Remove any remaining unresolved placeholders
        Pattern pattern = Pattern.compile("\\{[^}]+\\}");
        Matcher matcher = pattern.matcher(result);
        result = matcher.replaceAll("N/A"); // Replace unresolved placeholders with "N/A"
        return result;
    }

    // --- Notification Triggers ---

    /**
     * Notifies a specific user (e.g., client) via email and/or SMS based on their preferences.
     *
     * @param user The User object to notify.
     * @param templateName The name of the message template to use.
     * @param replacements List of String arrays, where each inner array is { "placeholderKey", "value" }.
     */
    public void notifyUser(User user, String templateName, List<String[]> replacements) {
        UserPreference preferences = Database.getUserPreferences(user.getId());
        // If no preferences exist, assume enabled by default
        boolean emailEnabled = (preferences == null) ? true : preferences.isEmailNotificationsEnabled();
        boolean smsEnabled = (preferences == null) ? true : preferences.isSmsNotificationsEnabled();

        MessageTemplate emailTemplate = Database.getMessageTemplateByName(templateName + "_Email");
        MessageTemplate smsTemplate = Database.getMessageTemplateByName(templateName + "_SMS");

        if (emailEnabled && emailTemplate != null) {
            String subject = replacePlaceholders(emailTemplate.getSubject(), replacements);
            String body = replacePlaceholders(emailTemplate.getBody(), replacements);
            sendEmail(user.getEmail(), subject, body);
        } else if (emailTemplate == null) {
            log("Warning: Email template '" + templateName + "_Email' not found for user " + user.getEmail());
        }

        if (smsEnabled && smsTemplate != null && user.getContact() != null && !user.getContact().isEmpty()) {
            // Assuming contact field in User model can be used for SMS (e.g., phone number)
            String message = replacePlaceholders(smsTemplate.getBody(), replacements);
            sendSMS(user.getContact(), message);
        } else if (smsTemplate == null) {
            log("Warning: SMS template '" + templateName + "_SMS' not found for user " + user.getEmail());
        } else if (user.getContact() == null || user.getContact().isEmpty()) {
            log("Warning: User " + user.getEmail() + " has no contact number for SMS.");
        }
    }

    /**
     * Notifies all users of a specific role (e.g., Committee members).
     *
     * @param role The role to notify (e.g., "Committee").
     * @param templateName The name of the message template to use.
     * @param replacements List of String arrays, where each inner array is { "placeholderKey", "value" }.
     */
    public void notifyByRole(String role, String templateName, List<String[]> replacements) {
        List<User> usersInRole = Database.getUsersByRole(role);
        for (User user : usersInRole) {
            notifyUser(user, templateName, replacements);
        }
    }

    /**
     * Notifies all users in the system.
     *
     * @param templateName The name of the message template to use.
     * @param replacements List of String arrays, where each inner array is { "placeholderKey", "value" }.
     */
    public void notifyAllUsers(String templateName, List<String[]> replacements) {
        List<User> allUsers = Database.getAllUsers();
        for (User user : allUsers) {
            notifyUser(user, templateName, replacements);
        }
    }

    // --- System Update Broadcast ---

    /**
     * Broadcasts a system update notification.
     *
     * @param update The SystemUpdate object.
     * @param targetRoles A list of roles to notify, or null/empty for all users.
     */
    public void broadcastSystemUpdate(SystemUpdate update, List<String> targetRoles) {
        Database.addSystemUpdate(update); // Persist the update

        List<String[]> replacements = List.of(
            new String[]{"version", update.getVersion()},
            new String[]{"title", update.getTitle()},
            new String[]{"message", update.getMessage()}
        );

        if (targetRoles == null || targetRoles.isEmpty()) {
            log("Broadcasting system update to ALL users: " + update.getTitle());
            notifyAllUsers("System_Update_AllUsers", replacements); // Use a generic template for all users
        } else {
            for (String role : targetRoles) {
                log("Broadcasting system update to role '" + role + "': " + update.getTitle());
                notifyByRole(role, "System_Update_AllUsers", replacements); // Can use same template, or specific ones
            }
        }
    }

    // --- Communication Settings Management ---
    public void saveEmailSettings(String host, int port, String username, String password, String senderEmail) {
        AppConfig.setProperty(AppConfig.SMTP_HOST_KEY, host);
        AppConfig.setIntProperty(AppConfig.SMTP_PORT_KEY, port);
        AppConfig.setProperty(AppConfig.SMTP_USERNAME_KEY, username);
        AppConfig.setProperty(AppConfig.SMTP_PASSWORD_KEY, password); // Store securely in production!
        AppConfig.setProperty(AppConfig.SENDER_EMAIL_KEY, senderEmail);
        log("Email settings saved.");
    }

    public void saveSmsSettings(String apiKey, String senderId) {
        AppConfig.setProperty(AppConfig.SMS_API_KEY, apiKey); // Store securely!
        AppConfig.setProperty(AppConfig.SMS_SENDER_ID_KEY, senderId);
        log("SMS settings saved.");
    }

    public void setAutoNotificationsEnabled(boolean enabled) {
        AppConfig.setBooleanProperty(AppConfig.AUTO_NOTIFICATIONS_ENABLED_KEY, enabled);
        log("Auto-notifications " + (enabled ? "enabled" : "disabled") + ".");
    }

    public List<MessageTemplate> getAllMessageTemplates() {
        return Database.getAllMessageTemplates();
    }

    public boolean updateMessageTemplate(MessageTemplate template) {
        boolean success = Database.updateMessageTemplate(template);
        if (success) {
            log("Message template '" + template.getTemplateName() + "' updated.");
        } else {
            log("Failed to update message template '" + template.getTemplateName() + "'.");
        }
        return success;
    }

    public List<MessageLog> getAllMessageLogs() {
        return Database.getAllMessageLogs();
    }
}