# Query Cassandra Java Application

A simple Java application to query Cassandra tables with configurable connection parameters.

## Features

- Query any Cassandra keyspace and table
- Configurable connection parameters via environment variables or system properties
- Proper dependency management with Maven
- Docker support with multi-stage builds

## Configuration

The application supports configuration via environment variables or JVM system properties:

| Environment Variable | System Property | Default Value | Description |
|---------------------|-----------------|---------------|-------------|
| `CASSANDRA_HOST` | `cassandra.host` | `localhost` | Cassandra host |
| `CASSANDRA_PORT` | `cassandra.port` | `9042` | Cassandra port |
| `CASSANDRA_USERNAME` | `cassandra.username` | `cassandra` | Username |
| `CASSANDRA_PASSWORD` | `cassandra.password` | (required) | Password |
| `CASSANDRA_DATACENTER` | `cassandra.datacenter` | `datacenter1` | Local datacenter |

## Usage

### Command Line
```bash
java -jar target/query-cassandra-1.0.0-fat.jar <keyspace> <table>
```

### Docker
```bash
docker run -e CASSANDRA_HOST=my-host -e CASSANDRA_PASSWORD=mypass asemjen/query-cassandra cycling races
```

### Environment Variables Example
```bash
export CASSANDRA_HOST=cassandra.example.com
export CASSANDRA_PASSWORD=mypassword
java -jar app.jar cycling races
```

### System Properties Example
```bash
java -Dcassandra.host=cassandra.example.com -Dcassandra.password=mypassword -jar app.jar cycling races
```

## Building

### Local Build
```bash
mvn clean package
```

### Docker Build
```bash
docker build -t query-cassandra .
```

## Project Structure

```
query-cassandra-java/
├── src/main/java/
│   └── QueryTable.java      # Main application class
├── pom.xml                  # Maven configuration
├── Dockerfile               # Multi-stage Docker build
├── .dockerignore           # Docker ignore patterns
└── README.md               # This file
```