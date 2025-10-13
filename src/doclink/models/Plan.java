package doclink.models;

import java.time.LocalDate;

public class Plan {
    private int id;
    private String applicantName;
    private String contact;
    private String plotNo;
    private String location;
    private LocalDate dateSubmitted;
    private String referenceNo;
    private String status; // e.g., Submitted, Awaiting Payment, Under Review (Planning), Approved
    private String remarks;

    public Plan(int id, String applicantName, String contact, String plotNo, String location, LocalDate dateSubmitted, String referenceNo, String status, String remarks) {
        this.id = id;
        this.applicantName = applicantName;
        this.contact = contact;
        this.plotNo = plotNo;
        this.location = location;
        this.dateSubmitted = dateSubmitted;
        this.referenceNo = referenceNo;
        this.status = status;
        this.remarks = remarks;
    }

    // Constructor for new plans (ID and refNo will be generated)
    public Plan(String applicantName, String contact, String plotNo, String location, LocalDate dateSubmitted, String status, String remarks) {
        this(0, applicantName, contact, plotNo, location, dateSubmitted, null, status, remarks);
    }

    // Getters
    public int getId() { return id; }
    public String getApplicantName() { return applicantName; }
    public String getContact() { return contact; }
    public String getPlotNo() { return plotNo; }
    public String getLocation() { return location; }
    public LocalDate getDateSubmitted() { return dateSubmitted; }
    public String getReferenceNo() { return referenceNo; }
    public String getStatus() { return status; }
    public String getRemarks() { return remarks; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public void setStatus(String status) { this.status = status; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}