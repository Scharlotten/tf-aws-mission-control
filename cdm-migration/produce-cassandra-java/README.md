# Race Data Producer Java Application

A Java application that continuously produces realistic cycling race data to the `cycling.races` table via ZDM Proxy. This tool is perfect for testing data migration and replication scenarios.

## Features

- Continuous data production (inserts every 4 seconds)
- Connects through ZDM Proxy for transparent routing
- Generates realistic race data with random:
  - Valid UUIDs for race IDs
  - Race names (e.g., "Tour de France", "Giro d'Italy")
  - Categories: Classic, Grand Tour, Stage Race
  - Countries: 10 European cycling nations
- Configurable connection parameters
- Graceful shutdown on interrupt (Ctrl+C)
- Production counter and logging

## Configuration

The application supports configuration via environment variables or JVM system properties:

| Environment Variable | System Property | Default Value | Description |
|---------------------|-----------------|---------------|-------------|
| `CASSANDRA_HOST` | `cassandra.host` | `zdm-proxy-service` | ZDM Proxy service host |
| `CASSANDRA_PORT` | `cassandra.port` | `9042` | CQL port |
| `CASSANDRA_USERNAME` | `cassandra.username` | `cassandra` | Username |
| `CASSANDRA_PASSWORD` | `cassandra.password` | (required) | Password |
| `CASSANDRA_DATACENTER` | `cassandra.datacenter` | `datacenter1` | Local datacenter |

## Sample Output

```
=== Cycling Race Data Producer ===
Connected to ZDM Proxy successfully!
Target: zdm-proxy-service:9042
Starting continuous data production...

[1] Inserted: ID=a7b8c9d0..., Name='Tour de France', Category='Grand Tour', Country='France'
[2] Inserted: ID=b1c2d3e4..., Name='Classic Belgium', Category='Classic', Country='Belgium'
[3] Inserted: ID=c5d6e7f8..., Name='Spain Championship', Category='Stage Race', Country='Spain'
...
```

## Usage

### Kubernetes Deployment
```bash
# Deploy the continuous producer
kubectl apply -f produce-race-data-deployment.yaml

# Monitor logs
kubectl logs -f deployment/produce-race-data -n hcd-epk60j1n

# Stop the producer
kubectl delete deployment produce-race-data -n hcd-epk60j1n
```

### Docker
```bash
docker run -e CASSANDRA_HOST=zdm-proxy-service -e CASSANDRA_PASSWORD=mypass asemjen/produce-cassandra
```

### Local Development
```bash
export CASSANDRA_HOST=localhost
export CASSANDRA_PASSWORD=mypassword
java -jar target/produce-cassandra-1.0.0-fat.jar
```

## Building

### Local Build
```bash
mvn clean package
```

### Docker Build
```bash
docker build -t produce-cassandra .
```

## Data Schema

Inserts data into the `cycling.races` table with schema:
```sql
CREATE TABLE cycling.races (
    id UUID PRIMARY KEY,
    name TEXT,
    category TEXT,
    country TEXT
);
```

## Project Structure

```
produce-cassandra-java/
├── src/main/java/
│   └── ProduceRaceData.java      # Main producer application
├── pom.xml                       # Maven configuration
├── Dockerfile                    # Multi-stage Docker build
├── produce-race-data-deployment.yaml  # Kubernetes deployment
└── README.md                     # This file
```

## Integration with ZDM Migration

This producer works perfectly with the ZDM setup:

1. **ZDM Proxy** routes writes to primary cluster (ORIGIN by default)
2. **CDM Migration** replicates data from origin to target
3. **Producer** generates continuous test data through the proxy
4. **Query tools** can verify data on both clusters

Perfect for testing end-to-end migration scenarios!