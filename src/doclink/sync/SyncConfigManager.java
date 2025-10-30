package doclink.sync;

import doclink.AppConfig;
import doclink.Database;
import doclink.models.Peer;
import doclink.models.ChangelogEntry; // NEW: Import ChangelogEntry
import doclink.models.Plan; // NEW: Import Plan
import doclink.models.User; // NEW: Import User
import doclink.models.Document; // NEW: Import Document

import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.sql.Connection; // NEW: Import Connection
import java.sql.SQLException; // NEW: Import SQLException
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors; // NEW: Import Collectors

public class SyncConfigManager {

    public enum SyncRole { SERVER, CLIENT, BOTH }
    public enum ConflictResolutionStrategy { LAST_WRITE_WINS, SERVER_WINS, CLIENT_WINS }
    public enum HybridSyncMode { P2P_ONLY, CENTRAL_API_ONLY, HYBRID }

    private SyncRole currentSyncRole;
    private HybridSyncMode currentHybridSyncMode;
    private boolean deleteLocalOnCentralPull; // NEW: Field for the new setting
    private ScheduledExecutorService scheduler;
    private Consumer<String> logConsumer; // For logging messages to the UI

    // Placeholder for network components
    private ServerSocket serverSocket;
    private DatagramSocket udpSocket; // Renamed from udpSocket to discoveryUdpSocket for clarity
    private Thread serverThread;
    private Thread udpDiscoveryThread;

    private static final int DISCOVERY_PORT = 8888; // Dedicated port for UDP discovery
    private static final int P2P_TCP_PORT = 8080; // Dedicated port for P2P TCP connections

    public SyncConfigManager(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
        loadSettings();
    }

    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
        System.out.println("[SyncConfigManager] " + message);
    }

    public void loadSettings() {
        currentSyncRole = SyncRole.valueOf(AppConfig.getProperty(AppConfig.SYNC_ROLE_KEY, SyncRole.CLIENT.name()));
        currentHybridSyncMode = HybridSyncMode.valueOf(AppConfig.getProperty(AppConfig.HYBRID_SYNC_MODE_KEY, HybridSyncMode.P2P_ONLY.name()));
        deleteLocalOnCentralPull = AppConfig.getBooleanProperty(AppConfig.DELETE_LOCAL_ON_CENTRAL_PULL_KEY, false); // NEW: Load the new setting
        log("Settings loaded. Role: " + currentSyncRole + ", Hybrid Mode: " + currentHybridSyncMode + ", Delete Local on Central Pull: " + deleteLocalOnCentralPull);
    }

    public void saveSettings() {
        AppConfig.setProperty(AppConfig.SYNC_ROLE_KEY, currentSyncRole.name());
        AppConfig.setProperty(AppConfig.HYBRID_SYNC_MODE_KEY, currentHybridSyncMode.name());
        AppConfig.setBooleanProperty(AppConfig.DELETE_LOCAL_ON_CENTRAL_PULL_KEY, deleteLocalOnCentralPull); // NEW: Save the new setting
        log("Settings saved.");
    }

    public SyncRole getCurrentSyncRole() {
        return currentSyncRole;
    }

    public void setSyncRole(SyncRole role) {
        if (this.currentSyncRole != role) {
            this.currentSyncRole = role;
            log("Sync role set to: " + role);
            restartSyncServices();
        }
    }

    public HybridSyncMode getCurrentHybridSyncMode() {
        return currentHybridSyncMode;
    }

    public void setHybridSyncMode(HybridSyncMode mode) {
        if (this.currentHybridSyncMode != mode) {
            this.currentHybridSyncMode = mode;
            log("Hybrid sync mode set to: " + mode);
            restartSyncServices();
        }
    }

    // NEW: Getter for deleteLocalOnCentralPull
    public boolean isDeleteLocalOnCentralPullEnabled() {
        return deleteLocalOnCentralPull;
    }

    // NEW: Setter for deleteLocalOnCentralPull
    public void setDeleteLocalOnCentralPull(boolean enabled) {
        if (this.deleteLocalOnCentralPull != enabled) {
            this.deleteLocalOnCentralPull = enabled;
            log("Delete local on central pull set to: " + enabled);
            saveSettings(); // Save immediately as this is a user preference
        }
    }

    public void startSyncServices() {
        stopSyncServices(); // Ensure clean restart

        log("Starting sync services...");
        scheduler = Executors.newScheduledThreadPool(2); // One for scheduled sync, one for UDP discovery

        if (currentSyncRole == SyncRole.SERVER || currentSyncRole == SyncRole.BOTH) {
            startServerThread();
            startUdpDiscoveryListener(); // Server also listens for discovery requests
        }
        if (currentSyncRole == SyncRole.CLIENT || currentSyncRole == SyncRole.BOTH) {
            startScheduledSync();
            // Client doesn't need a persistent listener for discovery, it initiates it.
        }

        if (currentHybridSyncMode == HybridSyncMode.CENTRAL_API_ONLY || currentHybridSyncMode == HybridSyncMode.HYBRID) {
            startCentralApiSync();
        }
        log("Sync services started based on configuration.");
    }

    public void stopSyncServices() {
        log("Stopping sync services...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            log("Scheduler shut down.");
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                log("Error closing server socket: " + e.getMessage());
            }
            log("Server thread interrupted.");
            serverThread = null; // Nullify the thread reference
        }
        if (udpDiscoveryThread != null && udpDiscoveryThread.isAlive()) {
            udpDiscoveryThread.interrupt();
            try {
                if (udpSocket != null) {
                    udpSocket.close(); // Close the discovery UDP socket
                    udpSocket = null; // Explicitly nullify the socket
                }
                // Give a small moment for the OS to release the port
                Thread.sleep(100); 
            } catch (Exception e) {
                log("Error closing UDP socket: " + e.getMessage());
            }
            log("UDP discovery listener thread interrupted.");
            udpDiscoveryThread = null; // Nullify the thread reference
        }
        log("All sync services stopped.");
    }

    private void restartSyncServices() {
        stopSyncServices();
        startSyncServices();
    }

    // --- Peer Management ---
    public List<Peer> getKnownPeers() {
        return Database.getAllPeers();
    }

    public boolean addPeer(String ip, int port, boolean trusted) {
        Peer newPeer = new Peer(ip, port, trusted, "Unknown");
        boolean success = Database.addPeer(newPeer);
        if (success) {
            log("Manually added peer: " + ip + ":" + port);
        } else {
            log("Failed to add peer: " + ip + ":" + port + " (might already exist)");
        }
        return success;
    }

    public boolean removePeer(int peerId) {
        boolean success = Database.deletePeer(peerId);
        if (success) {
            log("Removed peer with ID: " + peerId);
        } else {
            log("Failed to remove peer with ID: " + peerId);
        }
        return success;
    }

    public void discoverPeers() {
        log("Initiating UDP peer discovery...");
        List<Peer> discoveredPeers = new ArrayList<>();
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setBroadcast(true);
            clientSocket.setSoTimeout(5000); // Listen for 5 seconds

            String requestMessage = "DOCLINK_DISCOVERY_REQUEST";
            byte[] sendData = requestMessage.getBytes();
            
            // Send broadcast to all interfaces
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, DISCOVERY_PORT);
            clientSocket.send(sendPacket);
            log("Discovery request sent to " + broadcastAddress.getHostAddress() + ":" + DISCOVERY_PORT);

            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            boolean foundAny = false;

            while (true) {
                try {
                    clientSocket.receive(receivePacket); // Blocking call with timeout
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    if (response.startsWith("DOCLINK_DISCOVERY_RESPONSE:")) {
                        String[] parts = response.split(":");
                        if (parts.length == 3) {
                            String peerIp = receivePacket.getAddress().getHostAddress();
                            int peerPort = Integer.parseInt(parts[2]);
                            
                            // Avoid adding self
                            if (!peerIp.equals(InetAddress.getLocalHost().getHostAddress()) || peerPort != P2P_TCP_PORT) {
                                log("Discovered peer: " + peerIp + ":" + peerPort);
                                if (addPeer(peerIp, peerPort, false)) { // Add as untrusted by default
                                    discoveredPeers.add(new Peer(peerIp, peerPort, false, "Online"));
                                }
                                foundAny = true;
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    break; // No more responses within the timeout
                } catch (IOException e) {
                    log("Error during UDP discovery receive: " + e.getMessage());
                    break;
                }
            }

            if (foundAny) {
                log("UDP peer discovery completed successfully. Found " + discoveredPeers.size() + " new peers.");
            } else {
                log("UDP peer discovery completed. No new peers discovered.");
            }

        } catch (IOException e) {
            log("Error setting up UDP discovery: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Failed to initiate UDP discovery: " + e.getMessage(), "Discovery Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startUdpDiscoveryListener() {
        udpDiscoveryThread = new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0")); // Listen on all interfaces
                log("UDP discovery listener started on port " + DISCOVERY_PORT + ".");
                byte[] recvBuf = new byte[15000];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    udpSocket.receive(packet); // Blocking call
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (message.equals("DOCLINK_DISCOVERY_REQUEST")) {
                        String response = "DOCLINK_DISCOVERY_RESPONSE:" + InetAddress.getLocalHost().getHostAddress() + ":" + P2P_TCP_PORT;
                        byte[] sendData = response.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort()); // Corrected port to packet.getPort()
                        udpSocket.send(sendPacket);
                        log("Responded to discovery request from: " + packet.getAddress().getHostAddress());
                    }
                }
            } catch (SocketException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    log("UDP Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                log("UDP I/O error: " + e.getMessage());
            } finally {
                log("UDP discovery listener stopped.");
            }
        });
        udpDiscoveryThread.start();
    }


    // --- Sync Settings ---
    public int getSyncIntervalMinutes() {
        return AppConfig.getIntProperty(AppConfig.SYNC_INTERVAL_MINUTES_KEY, 5);
    }

    public void setSyncIntervalMinutes(int interval) {
        AppConfig.setIntProperty(AppConfig.SYNC_INTERVAL_MINUTES_KEY, interval);
        log("Sync interval set to: " + interval + " minutes.");
        restartSyncServices();
    }

    public ConflictResolutionStrategy getConflictResolutionStrategy() {
        return ConflictResolutionStrategy.valueOf(AppConfig.getProperty(AppConfig.CONFLICT_RESOLUTION_STRATEGY_KEY, ConflictResolutionStrategy.LAST_WRITE_WINS.name()));
    }

    public void setConflictResolutionStrategy(ConflictResolutionStrategy strategy) {
        AppConfig.setProperty(AppConfig.CONFLICT_RESOLUTION_STRATEGY_KEY, strategy.name());
        log("Conflict resolution strategy set to: " + strategy);
    }

    public int getChangelogRetentionDays() {
        return AppConfig.getIntProperty(AppConfig.CHANGELOG_RETENTION_DAYS_KEY, 30);
    }

    public void setChangelogRetentionDays(int days) {
        AppConfig.setIntProperty(AppConfig.CHANGELOG_RETENTION_DAYS_KEY, days);
        log("Changelog retention set to: " + days + " days.");
    }

    public boolean isAutoSyncEnabled() {
        return AppConfig.getBooleanProperty(AppConfig.AUTO_SYNC_ENABLED_KEY, true);
    }

    public void setAutoSyncEnabled(boolean enabled) {
        AppConfig.setBooleanProperty(AppConfig.AUTO_SYNC_ENABLED_KEY, enabled);
        log("Auto-sync " + (enabled ? "enabled" : "disabled") + ".");
        restartSyncServices();
    }

    public boolean isCompressionEnabled() {
        return AppConfig.getBooleanProperty(AppConfig.COMPRESSION_ENABLED_KEY, false);
    }

    public void setCompressionEnabled(boolean enabled) {
        AppConfig.setBooleanProperty(AppConfig.COMPRESSION_ENABLED_KEY, enabled);
        log("Compression " + (enabled ? "enabled" : "disabled") + ".");
    }

    public boolean isEncryptionEnabled() {
        return AppConfig.getBooleanProperty(AppConfig.ENCRYPTION_ENABLED_KEY, false);
    }

    public void setEncryptionEnabled(boolean enabled) {
        AppConfig.setBooleanProperty(AppConfig.ENCRYPTION_ENABLED_KEY, enabled);
        log("Encryption " + (enabled ? "enabled" : "disabled") + ".");
    }

    public String getCentralApiUrl() {
        return AppConfig.getProperty(AppConfig.CENTRAL_API_URL_KEY, "");
    }

    public void setCentralApiUrl(String url) {
        AppConfig.setProperty(AppConfig.CENTRAL_API_URL_KEY, url);
        log("Central API URL set to: " + url);
        restartSyncServices();
    }

    public String getCentralApiAuthToken() {
        return AppConfig.getProperty(AppConfig.CENTRAL_API_AUTH_TOKEN_KEY, "");
    }

    public void setCentralApiAuthToken(String token) {
        AppConfig.setProperty(AppConfig.CENTRAL_API_AUTH_TOKEN_KEY, token);
        log("Central API Auth Token updated.");
    }

    public boolean testCentralApiConnection() {
        String apiUrl = getCentralApiUrl();
        if (apiUrl.isEmpty()) {
            log("Central API URL is not configured.");
            return false;
        }
        log("Testing Central API connection to: " + apiUrl);
        // Placeholder for actual HTTP connection test
        // In a real implementation, you would make a small GET request
        // to a health check endpoint or similar.
        try {
            URL url = new URI(apiUrl).toURL(); // Use URI for safer parsing
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 5 seconds
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                log("Central API connection successful (HTTP 200 OK).");
                return true;
            } else {
                log("Central API connection failed. Response Code: " + responseCode);
                return false;
            }
        } catch (URISyntaxException e) {
            log("Error testing Central API connection: Invalid URI syntax - " + e.getMessage());
            return false;
        } catch (IOException e) {
            log("Error testing Central API connection: " + e.getMessage());
            return false;
        }
    }

    // --- Live Sync Monitor / Force Sync ---
    public void forceSyncNow() {
        log("Force sync initiated.");
        // This would trigger an immediate sync operation for all active modes (P2P, Central API)
        if (currentHybridSyncMode == HybridSyncMode.P2P_ONLY || currentHybridSyncMode == HybridSyncMode.HYBRID) {
            performP2PSync();
        }
        if (currentHybridSyncMode == HybridSyncMode.CENTRAL_API_ONLY || currentHybridSyncMode == HybridSyncMode.HYBRID) {
            performCentralApiSync();
        }
        // NEW: Also trigger local-to-central DB sync if applicable
        if (Database.getCurrentDatabaseType() == Database.DatabaseType.SQLITE && 
            (currentHybridSyncMode == HybridSyncMode.HYBRID || currentHybridSyncMode == HybridSyncMode.P2P_ONLY)) { // P2P_ONLY implies local DB is primary
            performLocalToCentralDbSync();
            performCentralToLocalDbSync(); // NEW: Also pull from central on force sync
        }
    }

    // --- Backend Sync Engine Placeholders ---

    private void startServerThread() {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(P2P_TCP_PORT); // Example port
                log("P2P Server started on port " + P2P_TCP_PORT + ".");
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept(); // Blocking call
                    log("Client connected from: " + clientSocket.getInetAddress().getHostAddress());
                    // Handle client connection in a new thread
                    new Thread(() -> handleClientConnection(clientSocket)).start();
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    log("P2P Server error: " + e.getMessage());
                }
            } finally {
                log("P2P Server stopped.");
            }
        });
        serverThread.start();
    }

    private void handleClientConnection(Socket clientSocket) {
        // Placeholder for server-side P2P communication logic
        // This would involve reading/writing change logs, applying updates,
        // and handling conflict resolution.
        try (clientSocket) {
            log("Handling P2P client connection from " + clientSocket.getInetAddress().getHostAddress());
            // Example: Read incoming changes
            // InputStream in = clientSocket.getInputStream();
            // OutputStream out = clientSocket.getOutputStream();
            // ... process changes ...
            log("P2P client connection handled for " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            log("Error handling client connection: " + e.getMessage());
        }
    }

    private void startScheduledSync() {
        if (!isAutoSyncEnabled()) {
            log("Auto-sync is disabled. Not starting scheduled P2P sync.");
            return;
        }
        int interval = getSyncIntervalMinutes();
        scheduler.scheduleAtFixedRate(() -> {
            performP2PSync();
            // NEW: Also trigger local-to-central DB sync if applicable
            if (Database.getCurrentDatabaseType() == Database.DatabaseType.SQLITE && 
                (currentHybridSyncMode == HybridSyncMode.HYBRID || currentHybridSyncMode == HybridSyncMode.P2P_ONLY)) {
                performLocalToCentralDbSync();
                performCentralToLocalDbSync(); // NEW: Also pull from central on scheduled sync
            }
        }, 0, interval, TimeUnit.MINUTES);
        log("Scheduled P2P sync to run every " + interval + " minutes.");
    }

    private void performP2PSync() {
        log("Performing P2P sync with known peers...");
        List<Peer> peers = Database.getAllPeers();
        for (Peer peer : peers) {
            if (peer.isTrusted()) { // Only sync with trusted peers for simplicity
                log("Syncing with peer: " + peer.getIpAddress() + ":" + peer.getPort());
                // Placeholder for actual P2P client-side communication logic
                // This would involve connecting to the peer's server, exchanging change logs,
                // and updating the peer's last_sync_time.
                try (Socket socket = new Socket(peer.getIpAddress(), peer.getPort())) {
                    // Example: Send outgoing changes
                    // OutputStream out = socket.getOutputStream();
                    // InputStream in = socket.getInputStream();
                    // ... exchange changes ...
                    peer.setLastSyncTime(LocalDateTime.now());
                    peer.setStatus("Online");
                    Database.updatePeer(peer);
                    log("Successfully synced with peer: " + peer.getIpAddress());
                } catch (IOException e) {
                    log("Failed to sync with peer " + peer.getIpAddress() + ":" + peer.getPort() + ": " + e.getMessage());
                    peer.setStatus("Offline");
                    Database.updatePeer(peer);
                }
            }
        }
        log("P2P sync cycle completed.");
    }

    private void startCentralApiSync() {
        if (!isAutoSyncEnabled()) {
            log("Auto-sync is disabled. Not starting scheduled Central API sync.");
            return;
        }
        int interval = getSyncIntervalMinutes();
        scheduler.scheduleAtFixedRate(this::performCentralApiSync, 0, interval, TimeUnit.MINUTES);
        log("Scheduled Central API sync to run every " + interval + " minutes.");
    }

    private void performCentralApiSync() {
        String apiUrl = getCentralApiUrl();
        String authToken = getCentralApiAuthToken();
        if (apiUrl.isEmpty()) {
            log("Central API URL is not configured. Skipping Central API sync.");
            return;
        }
        log("Performing Central API sync...");

        // Placeholder for actual Central API communication logic
        // 1. GET updates from /api/sync/updates?since=timestamp
        // 2. Apply incoming changes to local DB
        // 3. POST local changes to /api/sync/changes
        // 4. Handle responses and update local change log/last sync timestamp

        try {
            // Simulate GET updates
            log("Simulating GET updates from Central API...");
            URL getUrl = new URI(apiUrl + "/api/sync/updates?since=" + getLastCentralSyncTimestamp()).toURL(); // Uncommented
            // HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection();
            // getConn.setRequestMethod("GET");
            // getConn.setRequestProperty("Authorization", "Bearer " + authToken);
            // ... read response ...
            log("Simulated incoming changes applied.");

            // Simulate POST changes
            log("Simulating POST local changes to Central API...");
            URL postUrl = new URI(apiUrl + "/api/sync/changes").toURL(); // Uncommented
            // HttpURLConnection postConn = (HttpURLConnection) postUrl.openConnection();
            // postConn.setRequestMethod("POST");
            // postConn.setRequestProperty("Content-Type", "application/json");
            // postConn.setRequestProperty("Authorization", "Bearer " + authToken);
            // postConn.setDoOutput(true);
            // OutputStream os = postConn.getOutputStream();
            // os.write(getLocalChangesAsJson().getBytes()); // Replace with actual change log
            // os.flush();
            // ... read response ...
            log("Simulated outgoing changes sent.");

            // Update last sync timestamp
            // setLastCentralSyncTimestamp(LocalDateTime.now());
            log("Central API sync completed successfully.");

        } catch (URISyntaxException e) {
            log("Error during Central API sync: Invalid URI syntax - " + e.getMessage());
        } catch (Exception e) { // Catching generic Exception for now, can be refined to IOException
            log("Error during Central API sync: " + e.getMessage());
        }
    }

    // Placeholder for getting last central sync timestamp
    private LocalDateTime getLastCentralSyncTimestamp() {
        // In a real system, this would be stored persistently, e.g., in AppConfig or a dedicated table
        return LocalDateTime.MIN; // Or load from config
    }

    // Placeholder for getting local changes as JSON
    private String getLocalChangesAsJson() {
        // This would serialize your local change log into a JSON string
        return "[]";
    }

    // --- NEW: Local-to-Central Database Synchronization Logic (Push) ---
    public void performLocalToCentralDbSync() {
        String centralDbUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);
        if (centralDbUrl == null || centralDbUrl.trim().isEmpty()) {
            log("Central DB URL is not configured. Skipping local-to-central DB push sync.");
            return;
        }

        log("Initiating local SQLite to Central DB synchronization (Push)...");
        List<ChangelogEntry> unsyncedEntries = Database.getUnsyncedChangelogEntries();

        if (unsyncedEntries.isEmpty()) {
            log("No unsynced changes found in local changelog for push.");
            return;
        }

        try (Connection centralConn = Database.getCentralConnection()) {
            centralConn.setAutoCommit(false); // Start transaction for central DB
            int syncedCount = 0;
            for (ChangelogEntry entry : unsyncedEntries) {
                try {
                    boolean success = Database.applyChangelogEntryToCentralDb(entry, centralConn);
                    if (success) {
                        Database.markChangelogEntryAsSynced(entry.getId());
                        syncedCount++;
                    } else {
                        log("Failed to apply changelog entry " + entry.getId() + " to central DB. Will retry later.");
                        // Don't mark as synced, so it will be retried.
                    }
                } catch (SQLException e) {
                    log("SQL Error applying changelog entry " + entry.getId() + " to central DB: " + e.getMessage());
                    // Depending on conflict strategy, might try to resolve or just log and skip/retry
                    // For now, we just log and don't mark as synced.
                }
            }
            centralConn.commit(); // Commit all successful changes to central DB
            log("Local-to-Central DB push sync completed. Pushed " + syncedCount + " of " + unsyncedEntries.size() + " entries.");
        } catch (SQLException e) {
            log("Error connecting to or performing push operations on Central DB: " + e.getMessage());
            // Rollback if connection failed or commit failed
            try {
                Connection centralConn = Database.getCentralConnection(); // Re-establish connection for rollback if needed
                if (centralConn != null && !centralConn.getAutoCommit()) {
                    centralConn.rollback();
                    log("Central DB transaction rolled back due to push error.");
                }
            } catch (SQLException ex) {
                log("Error during central DB rollback after push: " + ex.getMessage());
            }
        }
    }

    // --- NEW: Central-to-Local DB Synchronization Logic (Pull) ---
    public void performCentralToLocalDbSync() {
        String centralDbUrl = AppConfig.getProperty(AppConfig.CENTRAL_DB_URL_KEY);
        if (centralDbUrl == null || centralDbUrl.trim().isEmpty()) {
            log("Central DB URL is not configured. Skipping central-to-local DB pull sync.");
            return;
        }

        log("Initiating Central DB to local SQLite synchronization (Pull)...");

        try (Connection centralConn = Database.getCentralConnection()) {
            // 1. Sync Users
            List<User> centralUsers = Database.getAllUsersFromCentralDb(centralConn);
            List<User> localUsers = Database.getAllUsersLocal(); // Get all local users
            Database.upsertUsersIntoLocalDb(centralUsers);
            log("Pulled and upserted " + centralUsers.size() + " users into local SQLite from central DB.");

            // Identify and delete users from local DB that are no longer in central DB
            if (deleteLocalOnCentralPull) { // NEW: Check the setting
                List<Integer> centralUserIds = centralUsers.stream().map(User::getId).collect(Collectors.toList());
                for (User localUser : localUsers) {
                    if (!centralUserIds.contains(localUser.getId())) {
                        Database.deleteUserLocalById(localUser.getId());
                        log("Deleted local user " + localUser.getId() + " as it no longer exists in central DB.");
                    }
                }
            } else {
                log("Local deletion of users on central pull is disabled. Skipping local user deletions.");
            }

            // 2. Sync Plans
            List<Plan> centralPlans = Database.getAllPlansFromCentralDb(centralConn);
            List<Plan> localPlans = Database.getAllPlansLocal(); // Get all local plans
            Database.upsertPlansIntoLocalDb(centralPlans);
            log("Pulled and upserted " + centralPlans.size() + " plans from central DB to local SQLite.");

            // Identify and delete plans from local DB that are no longer in central DB
            if (deleteLocalOnCentralPull) { // NEW: Check the setting
                List<Integer> centralPlanIds = centralPlans.stream().map(Plan::getId).collect(Collectors.toList());
                for (Plan localPlan : localPlans) {
                    if (!centralPlanIds.contains(localPlan.getId())) {
                        Database.deletePlanAndRelatedDataLocal(localPlan.getId()); // Use the cascading delete for plans
                        log("Deleted local plan " + localPlan.getId() + " and its related data as it no longer exists in central DB.");
                    }
                }
            } else {
                log("Local deletion of plans on central pull is disabled. Skipping local plan deletions.");
            }

            // 3. Sync Documents (NEW)
            List<Document> centralDocuments = Database.getAllDocumentsFromCentralDb(centralConn);
            List<Document> localDocuments = Database.getAllDocumentsLocal();
            Database.upsertDocumentsIntoLocalDb(centralDocuments);
            log("Pulled and upserted " + centralDocuments.size() + " documents from central DB to local SQLite.");

            // Identify and delete documents from local DB that are no longer in central DB
            if (deleteLocalOnCentralPull) {
                List<Integer> centralDocumentIds = centralDocuments.stream().map(Document::getId).collect(Collectors.toList());
                for (Document localDocument : localDocuments) {
                    if (!centralDocumentIds.contains(localDocument.getId())) {
                        Database.deleteDocumentLocalById(localDocument.getId());
                        log("Deleted local document " + localDocument.getId() + " as it no longer exists in central DB.");
                    }
                }
            } else {
                log("Local deletion of documents on central pull is disabled. Skipping local document deletions.");
            }

            // TODO: Extend this for other tables (billing, logs, meetings, etc.)
            // For each table, you'll need to:
            // a. Fetch all records from the central DB (e.g., Database.getAllDocumentsFromCentralDb(centralConn))
            // b. Fetch all records from the local DB (e.g., Database.getAllDocumentsLocal())
            // c. Upsert the central records into the local DB (e.g., Database.upsertDocumentsIntoLocalDb(centralDocuments))
            // d. If deleteLocalOnCentralPull is enabled, identify and delete local records that are not in the central records (e.g., Database.deleteDocumentLocalById(localDoc.getId()))

            // Update last central pull timestamp
            AppConfig.setProperty(AppConfig.LAST_CENTRAL_PULL_TIMESTAMP_KEY, LocalDateTime.now().toString());
            log("Central-to-Local DB pull sync completed successfully.");

        } catch (SQLException e) {
            log("Error connecting to or performing pull operations from Central DB: " + e.getMessage());
        }
    }
}