public class TestQueryTable {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TestQueryTable <keyspace> <table>");
            System.out.println("Example: java TestQueryTable cycling races");
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
        
        // Test configuration parsing
        System.out.println("=== Configuration Test ===");
        System.out.println("Keyspace: " + keyspace);
        System.out.println("Table: " + table);
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Username: " + username);
        System.out.println("Password: " + (password != null ? "***HIDDEN***" : "NOT SET"));
        System.out.println("Datacenter: " + datacenter);
        System.out.println();
        
        if (password == null) {
            System.out.println("⚠️  CASSANDRA_PASSWORD not set - this would fail in real usage");
        } else {
            System.out.println("✅ Configuration looks good!");
        }
        
        System.out.println("🚀 Would execute: SELECT * FROM " + keyspace + "." + table);
        System.out.println("📡 Would connect to: " + host + ":" + port + " (datacenter: " + datacenter + ")");
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