package doclink.models;

public class User {
    private int id;
    private String name;
    private String email;
    private String contact; // NEW: Added contact field
    private String role; // Reception, Planning, Committee, Director, Structural, Client

    public User(int id, String name, String email, String contact, String role) { // Updated constructor
        this.id = id;
        this.name = name;
        this.email = email;
        this.contact = contact; // Initialize new field
        this.role = role;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getContact() { return contact; } // NEW: Getter for contact
    public String getRole() { return role; }

    // Setters (if needed, e.g., for ID after insertion)
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setContact(String contact) { this.contact = contact; } // NEW: Setter for contact
    public void setRole(String role) { this.role = role; }
}