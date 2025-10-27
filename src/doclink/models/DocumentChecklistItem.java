package doclink.models;

public class DocumentChecklistItem {
    private int id;
    private String itemName;
    private boolean isRequired; // Future use: to mark if a document is mandatory
    private boolean requiresFileUpload; // NEW: To determine if a file upload is necessary

    public DocumentChecklistItem(int id, String itemName, boolean isRequired, boolean requiresFileUpload) {
        this.id = id;
        this.itemName = itemName;
        this.isRequired = isRequired;
        this.requiresFileUpload = requiresFileUpload;
    }

    // Constructor for new items (ID will be generated)
    public DocumentChecklistItem(String itemName, boolean isRequired, boolean requiresFileUpload) {
        this(0, itemName, isRequired, requiresFileUpload);
    }

    // Getters
    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public boolean isRequired() { return isRequired; }
    public boolean requiresFileUpload() { return requiresFileUpload; } // NEW Getter

    // Setters
    public void setId(int id) { this.id = id; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setRequired(boolean required) { isRequired = required; }
    public void setRequiresFileUpload(boolean requiresFileUpload) { this.requiresFileUpload = requiresFileUpload; } // NEW Setter
}