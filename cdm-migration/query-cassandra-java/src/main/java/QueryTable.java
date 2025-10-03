import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import java.net.InetSocketAddress;

public class QueryTable {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java QueryTable <keyspace> <table>");
            System.out.println("Example: java QueryTable cycling races");
            System.out.println();
            System.out.println("Configuration via environment variables or system properties:");
            System.out.println("  CASSANDRA_HOST or cassandra.host (default: localhost)");
            System.out.println("  CASSANDRA_PORT or cassandra.port (default: 9042)");
            System.out.println("  CASSANDRA_USERNAME or cassandra.username (default: cassandra)");
            System.out.println("  CASSANDRA_PASSWORD or cassandra.password (required)");
            System.out.println("  CASSANDRA_DATACENTER or cassandra.datacenter (default: datacenter1)");
            System.exit(1);
        }
        
        String keyspace = args[0];
        String table = args[1];
        
        // Configuration with environment variables and system properties fallback
        String host = getConfigValue("CASSANDRA_HOST", "cassandra.host", "localhost");
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
            
            System.out.println("Connected to Cassandra cluster successfully!");
            System.out.println("Querying " + keyspace + "." + table + " table:");
            
            ResultSet resultSet = session.execute("SELECT * FROM " + keyspace + "." + table);
            
            ColumnDefinitions columnDefinitions = resultSet.getColumnDefinitions();
            
            // Print column headers
            boolean first = true;
            for (ColumnDefinition column : columnDefinitions) {
                if (!first) System.out.print(" | ");
                System.out.print(column.getName().toString());
                first = false;
            }
            System.out.println();
            
            // Print separator line
            first = true;
            for (ColumnDefinition column : columnDefinitions) {
                if (!first) System.out.print("-+-");
                for (int i = 0; i < column.getName().toString().length(); i++) {
                    System.out.print("-");
                }
                first = false;
            }
            System.out.println();
            
            // Print rows
            for (Row row : resultSet) {
                first = true;
                for (ColumnDefinition column : columnDefinitions) {
                    if (!first) System.out.print(" | ");
                    Object value = row.getObject(column.getName());
                    System.out.print(value != null ? value.toString() : "null");
                    first = false;
                }
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Error connecting to Cassandra: " + e.getMessage());
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