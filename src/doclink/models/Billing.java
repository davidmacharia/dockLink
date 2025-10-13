package doclink.models;

import java.time.LocalDate;

public class Billing {
    private int id;
    private int planId;
    private double amount;
    private String receiptNo;
    private LocalDate datePaid;

    public Billing(int id, int planId, double amount, String receiptNo, LocalDate datePaid) {
        this.id = id;
        this.planId = planId;
        this.amount = amount;
        this.receiptNo = receiptNo;
        this.datePaid = datePaid;
    }

    // Constructor for new billing
    public Billing(int planId, double amount) {
        this(0, planId, amount, null, null);
    }

    // Getters
    public int getId() { return id; }
    public int getPlanId() { return planId; }
    public double getAmount() { return amount; }
    public String getReceiptNo() { return receiptNo; }
    public LocalDate getDatePaid() { return datePaid; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public void setDatePaid(LocalDate datePaid) { this.datePaid = datePaid; }
}