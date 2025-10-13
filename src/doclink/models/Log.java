package doclink.models;

import java.time.LocalDate;

public class Log {
    private int id;
    private int planId;
    private String fromRole;
    private String toRole;
    private String action;
    private String remarks;
    private LocalDate date;

    public Log(int id, int planId, String fromRole, String toRole, String action, String remarks, LocalDate date) {
        this.id = id;
        this.planId = planId;
        this.fromRole = fromRole;
        this.toRole = toRole;
        this.action = action;
        this.remarks = remarks;
        this.date = date;
    }

    // Constructor for new logs
    public Log(int planId, String fromRole, String toRole, String action, String remarks) {
        this(0, planId, fromRole, toRole, action, remarks, LocalDate.now());
    }

    // Getters
    public int getId() { return id; }
    public int getPlanId() { return planId; }
    public String getFromRole() { return fromRole; }
    public String getToRole() { return toRole; }
    public String getAction() { return action; }
    public String getRemarks() { return remarks; }
    public LocalDate getDate() { return date; }

    // Setters
    public void setId(int id) { this.id = id; }
}