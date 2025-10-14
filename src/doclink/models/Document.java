package doclink.models;

public class Document {
    private int id;
    private int planId;
    private String docName;
    private String filePath;
    private boolean isAttached;
    private String documentType; // e.g., "Submitted", "Generated"

    public Document(int id, int planId, String docName, String filePath, boolean isAttached, String documentType) {
        this.id = id;
        this.planId = planId;
        this.docName = docName;
        this.filePath = filePath;
        this.isAttached = isAttached;
        this.documentType = documentType;
    }

    // Constructor for new submitted documents
    public Document(String docName, String filePath, boolean isAttached) {
        this(0, 0, docName, filePath, isAttached, "Submitted");
    }

    // Constructor for new generated documents
    public Document(int planId, String docName, String filePath, String documentType) {
        this(0, planId, docName, filePath, true, documentType); // Generated documents are always attached
    }

    // Getters
    public int getId() { return id; }
    public int getPlanId() { return planId; }
    public String getDocName() { return docName; }
    public String getFilePath() { return filePath; }
    public boolean isAttached() { return isAttached; }
    public String getDocumentType() { return documentType; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPlanId(int planId) { this.planId = planId; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setAttached(boolean attached) { isAttached = attached; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
}