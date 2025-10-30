package doclink.models;

import java.time.LocalDateTime;

public class ChangelogEntry {
    private int id;
    private String tableName;
    private int recordId; // ID of the record in the table that was changed
    private String changeType; // e.g., "INSERT", "UPDATE", "DELETE"
    private String columnName; // For UPDATE, which column changed
    private String oldValue;   // For UPDATE, the old value
    private String newValue;   // For INSERT/UPDATE, the new value
    private LocalDateTime timestamp;
    private boolean isSynced;

    // Constructor for INSERT/DELETE (columnName, oldValue can be null)
    public ChangelogEntry(int id, String tableName, int recordId, String changeType, LocalDateTime timestamp, boolean isSynced) {
        this(id, tableName, recordId, changeType, null, null, null, timestamp, isSynced);
    }

    // Constructor for UPDATE
    public ChangelogEntry(int id, String tableName, int recordId, String changeType, String columnName, String oldValue, String newValue, LocalDateTime timestamp, boolean isSynced) {
        this.id = id;
        this.tableName = tableName;
        this.recordId = recordId;
        this.changeType = changeType;
        this.columnName = columnName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = timestamp;
        this.isSynced = isSynced;
    }

    // Constructor for new entries (ID and timestamp will be generated, not synced initially)
    public ChangelogEntry(String tableName, int recordId, String changeType) {
        this(0, tableName, recordId, changeType, null, null, null, LocalDateTime.now(), false);
    }

    public ChangelogEntry(String tableName, int recordId, String changeType, String columnName, String oldValue, String newValue) {
        this(0, tableName, recordId, changeType, columnName, oldValue, newValue, LocalDateTime.now(), false);
    }

    // Getters
    public int getId() { return id; }
    public String getTableName() { return tableName; }
    public int getRecordId() { return recordId; }
    public String getChangeType() { return changeType; }
    public String getColumnName() { return columnName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSynced() { return isSynced; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSynced(boolean synced) { isSynced = synced; }
}