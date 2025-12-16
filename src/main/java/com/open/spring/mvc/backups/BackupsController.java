package com.open.spring.mvc.backups;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.open.spring.mvc.slack.CalendarEvent;
import com.open.spring.mvc.slack.CalendarEventService;
import com.open.spring.mvc.bathroom.Tinkle;
import com.open.spring.mvc.bathroom.TinkleJPARepository;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;
import com.open.spring.mvc.groups.Groups;
import com.open.spring.mvc.groups.GroupsJpaRepository;
import com.open.spring.mvc.bank.Bank;
import com.open.spring.mvc.bank.BankJpaRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Consolidated controller for all backup operations.
 * Handles both full database exports and specific API endpoint backups.
 */
@RestController
@RequestMapping("/api/exports")
@Component
public class BackupsController {

    // ========== FULL DATABASE BACKUP CONFIGURATION ==========
    private static final String DB_PATH = "./volumes/sqlite.db";
    private static final String BACKUP_DIR = "./volumes/backups/";
    
    // ========== SPECIFIC ENDPOINT BACKUP CONFIGURATION ==========
    @Value("${backup.base.path:./backups}")
    private String backupBasePath;

    @Value("${backup.max.files:3}")
    private int maxBackupFiles;

    @Value("${server.port:8080}")
    private String serverPort;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // ObjectMapper for JSON serialization
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CalendarEventService calendarEventService;

    @Autowired
    private TinkleJPARepository tinkleRepository;

    @Autowired
    private PersonJpaRepository personRepository;

    @Autowired
    private GroupsJpaRepository groupsRepository;

    @Autowired
    private BankJpaRepository bankRepository;

    // Configuration for API endpoints and their corresponding directories
    private final List<BackupEndpoint> endpoints = Arrays.asList(
        new BackupEndpoint("/api/people/bulk/extract", "person"),
        new BackupEndpoint("/api/groups/bulk/extract", "groups"),
        new BackupEndpoint("/api/tinkle/bulk/extract", "tinkle"),
        new BackupEndpoint("/api/calendar/events", "calendar"),
        new BackupEndpoint("/bank/bulk/extract", "bank")
    );

    // ========== FULL DATABASE BACKUP ENDPOINTS ==========

    /**
     * Endpoint to retrieve data from all tables in the database
     */
    @GetMapping("/getAll")
    public void getAllTablesData(HttpServletResponse response) {
        // Export data from the database
        Map<String, List<Map<String, Object>>> data = exportData();

        // Set response headers for file download
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=exports.json");

        // Write the JSON data to the response output stream
        try (OutputStream out = response.getOutputStream()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, data);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to stream JSON data to the client", e);
        }
    }

    /**
     * Method to export data from the database
     */
    private Map<String, List<Map<String, Object>>> exportData() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             Statement statement = connection.createStatement()) {

            // Get the list of tables in the database
            List<String> tableNames = getTableNames(connection);

            // Loop through each table and retrieve its data
            for (String tableName : tableNames) {
                List<Map<String, Object>> tableData = getTableData(statement, tableName);
                result.put(tableName, tableData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve data from the database", e);
        }

        return result;
    }

    /**
     * Helper method to get the list of table names in the database
     */
    private List<String> getTableNames(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();

        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
        }

        return tableNames;
    }

    /**
     * Helper method to retrieve data from a specific table
     */
    private List<Map<String, Object>> getTableData(Statement statement, String tableName) throws SQLException {
        List<Map<String, Object>> tableData = new ArrayList<>();

        try (ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object columnValue = resultSet.getObject(i);

                    // Handle special fields (e.g., stats, kasm_server_needed)
                    if (columnName.equals("stats") && columnValue instanceof String) {
                        // If stats is stored as a string, parse it as JSON
                        columnValue = objectMapper.readValue((String) columnValue, Map.class);
                    } else if (columnName.equals("kasm_server_needed") && columnValue instanceof Integer) {
                        // If kasm_server_needed is stored as an integer, convert it to boolean
                        columnValue = ((Integer) columnValue) == 1;
                    }

                    row.put(columnName, columnValue);
                }
                tableData.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException("Failed to retrieve data from table: " + tableName, e);
        }

        return tableData;
    }

    // ========== SHUTDOWN BACKUP OPERATIONS ==========

    /**
     * This method will be called just before the server stops.
     * Performs both full database backup and specific endpoint backups.
     */
    @EventListener
    public void handleContextClose(ContextClosedEvent event) {
        System.out.println("Server is stopping. Starting backup process...");
        
        // Perform full database backup
        performFullDatabaseBackup();
        
        // Perform specific endpoint backups
        performSpecificEndpointBackups();
        
        System.out.println("All backup operations completed.");
    }

    /**
     * Perform full database backup on shutdown
     */
    private void performFullDatabaseBackup() {
        System.out.println("Exporting full database...");

        // Export data
        Map<String, List<Map<String, Object>>> data = exportData();

        // Save the data to a JSON file with a timestamp
        saveFullDatabaseBackup(data);

        // Manage backups to keep only the three most recent ones
        manageFullDatabaseBackups();

        System.out.println("Full database backup completed.");
    }

    /**
     * Perform specific endpoint backups on shutdown
     */
    private void performSpecificEndpointBackups() {
        System.out.println("Backing up specific endpoints...");
        
        for (BackupEndpoint endpoint : endpoints) {
            try {
                backupEndpoint(endpoint);
            } catch (Exception e) {
                System.err.println("Failed to backup endpoint " + endpoint.getPath() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Specific endpoint backups completed.");
    }

    /**
     * Helper method to save the full database JSON data to a file with a timestamp
     */
    private void saveFullDatabaseBackup(Map<String, List<Map<String, Object>>> data) {
        try {
            // Create the backups directory if it doesn't exist
            File backupsDir = new File(BACKUP_DIR);
            if (!backupsDir.exists()) {
                backupsDir.mkdirs();
            }

            // Generate a timestamp for the filename
            String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileName = "backup_" + timeStamp + ".json";
            File jsonFile = new File(backupsDir, fileName);

            // Write the JSON data to the file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, data);
            System.out.println("Full database backup saved to: " + jsonFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save JSON data to file", e);
        }
    }

    /**
     * Helper method to manage full database backups, keeping only the three most recent ones
     */
    private void manageFullDatabaseBackups() {
        File backupsDir = new File(BACKUP_DIR);
        if (!backupsDir.exists() || !backupsDir.isDirectory()) {
            return;
        }

        // Get all backup files
        File[] backupFiles = backupsDir.listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".json"));

        if (backupFiles == null || backupFiles.length <= 3) {
            return;
        }

        // Sort files by last modified date (oldest first)
        Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));

        // Delete the oldest files if there are more than three
        for (int i = 0; i < backupFiles.length - 3; i++) {
            if (backupFiles[i].delete()) {
                System.out.println("Deleted old full database backup: " + backupFiles[i].getName());
            } else {
                System.out.println("Failed to delete old full database backup: " + backupFiles[i].getName());
            }
        }
    }

    /**
     * Backup a specific endpoint to a JSON file
     * All endpoints are backed up directly via repositories/services to avoid authentication issues
     */
    private void backupEndpoint(BackupEndpoint endpoint) throws IOException {
        String jsonResponse;
        
        try {
            System.out.println("Backing up " + endpoint.getPath() + " via direct repository/service access...");
            
            // Handle each endpoint type directly via repositories/services
            if ("/api/calendar/events".equals(endpoint.getPath())) {
                List<CalendarEvent> events = calendarEventService.getAllEvents();
                jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(events);
            } else if ("/api/tinkle/bulk/extract".equals(endpoint.getPath())) {
                List<Tinkle> tinkleList = tinkleRepository.findAll();
                List<Map<String, String>> tinkleDtos = new ArrayList<>();
                for (Tinkle tinkle : tinkleList) {
                    Map<String, String> dto = new HashMap<>();
                    dto.put("sid", tinkle.getSid());
                    dto.put("timeIn", tinkle.getTimeIn());
                    tinkleDtos.add(dto);
                }
                jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tinkleDtos);
            } else if ("/api/people/bulk/extract".equals(endpoint.getPath())) {
                List<Person> people = personRepository.findAllByOrderByNameAsc();
                List<Map<String, Object>> personDtos = new ArrayList<>();
                for (Person person : people) {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("email", person.getEmail());
                    dto.put("uid", person.getUid());
                    dto.put("sid", person.getSid());
                    dto.put("password", person.getPassword());
                    dto.put("name", person.getName());
                    dto.put("pfp", person.getPfp());
                    dto.put("kasmServerNeeded", person.getKasmServerNeeded());
                    personDtos.add(dto);
                }
                jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(personDtos);
            } else if ("/api/groups/bulk/extract".equals(endpoint.getPath())) {
                List<Groups> groups = groupsRepository.findAll();
                List<Map<String, Object>> groupsList = new ArrayList<>();
                for (Groups group : groups) {
                    Map<String, Object> groupMap = new HashMap<>();
                    groupMap.put("id", group.getId());
                    groupMap.put("name", group.getName());
                    groupMap.put("period", group.getPeriod());
                    
                    // Extract basic info for each member
                    List<Map<String, Object>> membersList = new ArrayList<>();
                    for (Person person : group.getGroupMembers()) {
                        Map<String, Object> personInfo = new HashMap<>();
                        personInfo.put("id", person.getId());
                        personInfo.put("uid", person.getUid());
                        personInfo.put("name", person.getName());
                        personInfo.put("email", person.getEmail());
                        membersList.add(personInfo);
                    }
                    groupMap.put("members", membersList);
                    groupsList.add(groupMap);
                }
                jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(groupsList);
            } else if ("/bank/bulk/extract".equals(endpoint.getPath())) {
                List<Bank> bankList = bankRepository.findAll();
                List<Map<String, Object>> bankDtos = new ArrayList<>();
                for (Bank bank : bankList) {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", bank.getId());
                    dto.put("username", bank.getUsername());
                    dto.put("uid", bank.getUid());
                    dto.put("balance", bank.getBalance());
                    dto.put("loanAmount", bank.getLoanAmount());
                    dto.put("dailyInterestRate", bank.getDailyInterestRate());
                    dto.put("riskCategory", bank.getRiskCategory());
                    if (bank.getPerson() != null) {
                        dto.put("personId", bank.getPerson().getId());
                    }
                    bankDtos.add(dto);
                }
                jsonResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bankDtos);
            } else {
                throw new RuntimeException("Unknown endpoint: " + endpoint.getPath());
            }
            
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                System.out.println("Empty response from " + endpoint.getPath() + ", skipping backup");
                return;
            }

            // Validate JSON
            objectMapper.readTree(jsonResponse);
            
            // Create directory structure
            Path backupDir = createBackupDirectory(endpoint.getDirectoryName());
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String filename = endpoint.getDirectoryName() + "_backup_" + timestamp + ".json";
            Path filePath = backupDir.resolve(filename);
            
            // Write JSON to file
            Files.write(filePath, jsonResponse.getBytes());
            System.out.println("Backup saved: " + filePath);
            
            // Manage file rotation
            manageFileRotation(backupDir, endpoint.getDirectoryName());
            
        } catch (Exception e) {
            System.err.println("Error during backup of " + endpoint.getPath() + ": " + e.getMessage());
            throw new RuntimeException("Backup failed for " + endpoint.getPath(), e);
        }
    }

    /**
     * Create backup directory for specific endpoint backups
     */
    private Path createBackupDirectory(String subdirectory) throws IOException {
        Path backupPath = Paths.get(backupBasePath, subdirectory);
        
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
            System.out.println("Created backup directory: " + backupPath.toString());
        }
        
        return backupPath;
    }

    /**
     * Manage file rotation for specific endpoint backups
     */
    private void manageFileRotation(Path directory, String prefix) throws IOException {
        File[] files = directory.toFile().listFiles((dir, name) -> 
            name.startsWith(prefix + "_backup_") && name.endsWith(".json"));
        
        if (files == null) return;
        
        if (files.length > maxBackupFiles) {
            // Sort files by last modified time (oldest first)
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            
            // Delete oldest files to maintain the limit
            int filesToDelete = files.length - maxBackupFiles;
            for (int i = 0; i < filesToDelete; i++) {
                boolean deleted = files[i].delete();
                if (deleted) {
                    System.out.println("Deleted old endpoint backup: " + files[i].getName());
                } else {
                    System.err.println("Failed to delete old endpoint backup: " + files[i].getName());
                }
            }
        }
    }

    // ========== INNER CLASSES ==========

    /**
     * Inner class to hold endpoint configuration for specific backups
     */
    private static class BackupEndpoint {
        private final String path;
        private final String directoryName;

        public BackupEndpoint(String path, String directoryName) {
            this.path = path;
            this.directoryName = directoryName;
        }

        public String getPath() {
            return path;
        }

        public String getDirectoryName() {
            return directoryName;
        }
    }
}

/**
 * Configuration class for RestTemplate bean
 */
@Component
class BackupConfiguration {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add timeout configuration
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setConnectionRequestTimeout(10000);
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }
}