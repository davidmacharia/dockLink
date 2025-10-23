package doclink.models;

import java.time.LocalDate;

public class Meeting {
    private int id;
    private String title;
    private LocalDate date;
    private String time; // e.g., "10:00 AM"
    private String location;
    private String agenda;
    private String status; // e.g., Scheduled, Cancelled, Completed

    public Meeting(int id, String title, LocalDate date, String time, String location, String agenda, String status) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.location = location;
        this.agenda = agenda;
        this.status = status;
    }

    // Constructor for new meetings (ID will be generated)
    public Meeting(String title, LocalDate date, String time, String location, String agenda, String status) {
        this(0, title, date, time, location, agenda, status);
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public String getLocation() { return location; }
    public String getAgenda() { return agenda; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setLocation(String location) { this.location = location; }
    public void setAgenda(String agenda) { this.agenda = agenda; }
    public void setStatus(String status) { this.status = status; }
}