package doclink.models;

import java.time.LocalDateTime;

public class Peer {
    private int id;
    private String ipAddress;
    private int port;
    private LocalDateTime lastSyncTime;
    private boolean isTrusted;
    private String status; // e.g., 'Online', 'Offline', 'Syncing', 'Unknown'

    public Peer(int id, String ipAddress, int port, LocalDateTime lastSyncTime, boolean isTrusted, String status) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastSyncTime = lastSyncTime;
        this.isTrusted = isTrusted;
        this.status = status;
    }

    // Constructor for new peers (ID and lastSyncTime will be generated/set later)
    public Peer(String ipAddress, int port, boolean isTrusted, String status) {
        this(0, ipAddress, port, null, isTrusted, status);
    }

    // Getters
    public int getId() { return id; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public boolean isTrusted() { return isTrusted; }
    public String getStatus() { return status; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setPort(int port) { this.port = port; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public void setTrusted(boolean trusted) { isTrusted = trusted; }
    public void setStatus(String status) { this.status = status; }
}