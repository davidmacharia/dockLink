package doclink.models;

public class Document {
    private int id;
    private int planId;
    private String docName;
    private String filePath;
    private boolean isAttached;

    public Document(int id, int planId, String docName, String filePath, boolean isAttached) {
        this.id = id;
        this.planId = planId;
        this.docName = docName;
        this.filePath = filePath;
        this.isAttached = isAttached;
    }

    // Constructor for new documents
    public Document(String docName, String filePath, boolean isAttached) {
        this(0, 0, docName, filePath, isAttached);
    }

    // Getters
    public int getId() { return id; }
    public int getPlanId() { return planId; }
    public String getDocName() { return docName; }
    public String getFilePath() { return filePath; }
    public boolean isAttached() { return isAttached; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPlanId(int planId) { this.planId = planId; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setAttached(boolean attached) { isAttached = attached; }
}