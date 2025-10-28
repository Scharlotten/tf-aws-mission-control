import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ProduceRaceData {
    // Race data arrays for randomization
    private static final String[] CATEGORIES = {"Classic", "Grand Tour", "Stage Race"};
    private static final String[] COUNTRIES = {"France", "Italy", "Spain", "Belgium", "Netherlands", "Switzerland", "United Kingdom", "Germany", "Denmark", "Norway"};
    private static final String[] RACE_NAMES = {
        "Tour de %s", "Vuelta a %s", "Giro d'%s", "Classic %s", 
        "%s Championship", "Grand Prix %s", "%s Masters", "Circuit %s",
        "%s Trophy", "Memorial %s", "%s Challenge", "Criterium %s"
    };
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("=== Cycling Race Data Producer ===");
        System.out.println("This application continuously inserts race data into the cycling.races table via ZDM Proxy");
        System.out.println("Data is inserted every 4 seconds. Press Ctrl+C to stop.");
        System.out.println();
        System.out.println("Configuration via environment variables or system properties:");
        System.out.println("  CASSANDRA_HOST or cassandra.host (default: zdm-proxy-service)");
        System.out.println("  CASSANDRA_PORT or cassandra.port (default: 9042)");
        System.out.println("  CASSANDRA_USERNAME or cassandra.username (default: cassandra)");
        System.out.println("  CASSANDRA_PASSWORD or cassandra.password (required)");
        System.out.println("  CASSANDRA_DATACENTER or cassandra.datacenter (default: datacenter1)");
        System.out.println();
        
        // Configuration with environment variables and system properties fallback
        String host = getConfigValue("CASSANDRA_HOST", "cassandra.host", "zdm-proxy-service");
        int port = Integer.parseInt(getConfigValue("CASSANDRA_PORT", "cassandra.port", "9042"));
        String username = getConfigValue("CASSANDRA_USERNAME", "cassandra.username", "cassandra");
        String password = getConfigValue("CASSANDRA_PASSWORD", "cassandra.password", null);
        String datacenter = getConfigValue("CASSANDRA_DATACENTER", "cassandra.datacenter", "datacenter1");
        
        if (password == null) {
            System.out.println("Please set CASSANDRA_PASSWORD environment variable");
            System.exit(1);
        }
        
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withAuthCredentials(username, password)
                .withLocalDatacenter(datacenter)
                .build()) {
            
            System.out.println("Connected to ZDM Proxy successfully!");
            System.out.println("Target: " + host + ":" + port);
            System.out.println("Starting continuous data production...");
            System.out.println();
            
            // Prepare insert statement for cycling.races table
            String insertQuery = "INSERT INTO cycling.races (id, name, category, country) VALUES (?, ?, ?, ?)";
            PreparedStatement preparedStatement = session.prepare(insertQuery);
            
            int insertCount = 0;
            
            // Continuous data production loop
            while (true) {
                try {
                    // Generate random race data
                    UUID raceId = UUID.randomUUID();
                    String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
                    String nameTemplate = RACE_NAMES[random.nextInt(RACE_NAMES.length)];
                    String raceName = String.format(nameTemplate, country);
                    String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
                    
                    // Insert the race data
                    session.execute(preparedStatement.bind(raceId, raceName, category, country));
                    insertCount++;
                    
                    // Log the inserted data
                    System.out.printf("[%d] Inserted: ID=%s, Name='%s', Category='%s', Country='%s'%n",
                            insertCount, raceId.toString().substring(0, 8) + "...", raceName, category, country);
                    
                    // Wait 4 seconds before next insert
                    TimeUnit.SECONDS.sleep(4);
                    
                } catch (InterruptedException e) {
                    System.out.println("\nReceived interrupt signal. Shutting down gracefully...");
                    break;
                } catch (Exception e) {
                    System.err.println("Error inserting data: " + e.getMessage());
                    // Continue running even if individual inserts fail
                }
            }
            
            System.out.println("Data producer stopped. Total records inserted: " + insertCount);
            
        } catch (Exception e) {
            System.err.println("Error connecting to ZDM Proxy: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets configuration value from environment variable first, then system property, then default value.
     */
    private static String getConfigValue(String envVar, String sysProp, String defaultValue) {
        String value = System.getenv(envVar);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        
        value = System.getProperty(sysProp);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        
        return defaultValue;
    }
}