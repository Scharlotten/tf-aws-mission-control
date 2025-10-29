
# Cassandra Data Migration (CDM) Setup

This directory contains the necessary files and configurations to set up a Cassandra Data Migrator (CDM) migration between HyperConverged Database (HCD) and Apache Cassandra clusters.

## Overview

The Cassandra Data Migrator enables seamless data migration between different Cassandra deployments, supporting both Apache Cassandra and DataStax HCD environments. This setup demonstrates migrating data from a source Cassandra cluster to a target HCD cluster.

## Prerequisites

- Kubernetes cluster with sufficient resources
- Valid namespace/project for deployment
- Source and target keyspaces and tables pre-created
- Appropriate RBAC permissions

## Architecture

```
┌─────────────────┐    CDM Job    ┌─────────────────┐
│ Source:         │ ──────────────→ │ Target:         │
│ Apache          │               │ DataStax HCD    │
│ Cassandra       │               │                 │
│ (cycling.races) │               │ (cycling.races) │
└─────────────────┘               └─────────────────┘
```

## Files Description

| File/Directory | Purpose | Description |
|------|---------|-------------|
| `db-definitions/cassandra.yaml` | Source Cluster | Apache Cassandra 3.11.15 MissionControlCluster manifest |
| `db-definitions/hcd.yaml` | Target Cluster | DataStax HCD 1.2.3 MissionControlCluster manifest |
| `cdm-config/cdm-migration.yaml` | Migration Job | CDM configuration and Kubernetes Job for data migration |
| `cdm-config/cdm-migration-job-with-pvc.yaml` | Migration Job with Persistence | CDM Job with PersistentVolume for state and log persistence |
| `cdm-config/cdm-job-pvc-pickup.yaml` | Debug Pod | Ubuntu debug pod to access PVC logs and data after job completion |
| `query-cassandra-java/` | Testing Tool | Java application for database connectivity testing |
| `produce-cassandra-java/` | Data Generation | Java application for producing test data to Cassandra |
| `zdm-proxy/` | ZDM Proxy | Zero Downtime Migration proxy configuration and testing tools |

## Quick Start

### 1. Deploy Database Clusters

**Important:** Update the `namespace` field in both manifests to match your environment.

Deploy the source Apache Cassandra cluster:
```bash
kubectl apply -f db-definitions/cassandra.yaml
```

Deploy the target HCD cluster:
```bash
kubectl apply -f db-definitions/hcd.yaml
```

### 2. Verify Cluster Deployment

Check cluster status:
```bash
kubectl get missioncontrolcluster -n <your-namespace>
kubectl get pods -n <your-namespace>
```

Wait for all pods to be in `Running` state before proceeding.

### 3. Configure Migration Parameters

Edit `cdm-config/cdm-migration.yaml` and update the following:

- **Namespace**: Change `namespace: hcd-epk60j1n` to your namespace
- **Passwords**: Replace `yourPassword` with actual superuser passwords
- **Service Names**: Verify service DNS names match your deployment:
  ```bash
  kubectl get svc -n <your-namespace>
  ```

### 4. Execute Migration

**Option A: Basic Job (No Persistence)**
```bash
kubectl apply -f cdm-config/cdm-migration.yaml
kubectl logs -f job/cassandra-data-migrator -n <your-namespace>
```

**Option B: Job with Persistent Volume (Recommended for Large Migrations)**
```bash
# Deploy job with PVC for state persistence and resumability
kubectl apply -f cdm-config/cdm-migration-job-with-pvc.yaml
kubectl logs -f job/cassandra-data-migrator-with-pvc -n <your-namespace>
```

Both jobs auto-cleanup after 5 minutes of completion to keep the cluster clean.

## Configuration Details

### CDM Properties Reference

The migration uses the following key configurations:

| Property | Value | Description |
|----------|-------|-------------|
| `spark.cdm.connect.origin.host` | `cassandra-cassandra-dc-1-contact-points-service...` | Source cluster service |
| `spark.cdm.connect.target.host` | `hcd-hcd-dc-1-contact-points-service...` | Target cluster service |
| `spark.cdm.schema.origin.keyspaceTable` | `cycling.races` | Source table to migrate |
| `spark.cdm.perfops.batchSize` | `1000` | Migration batch size |
| `spark.cdm.perfops.numSplits` | `8` | Parallel processing splits |

### Resource Allocation

The migration job is configured with:
- **CPU**: 1-2 cores
- **Memory**: 2-4 GiB
- **Java Heap**: 1-2 GiB

## Testing Connection

Use the included Java application to test database connectivity:

```bash
cd query-cassandra-java
kubectl apply -f query-cassandra-deployment.yaml
```

See [query-cassandra-java/README.md](query-cassandra-java/README.md) for detailed usage instructions.

## Accessing Persistent Logs and Data

If you used the PVC-enabled job (`cdm-migration-job-with-pvc.yaml`), all logs and state are saved to the persistent volume. After the job completes and the pod is cleaned up (after 5 minutes), you can still access the data:

### Deploy Debug Pod

```bash
# Deploy Ubuntu debug pod to access PVC
kubectl apply -f cdm-config/cdm-job-pvc-pickup.yaml
```

### Access Logs and Data

```bash
# Connect to debug pod with full bash environment
kubectl exec -it cdm-debug -- bash

# Navigate to data directory (everything is under /data)
cd /data

# View migration logs
cat logs/cdm-migration.log
tail -100 logs/cdm-migration.log

# Check job completion status
cat logs/last-run-id.txt
cat logs/last-exit-code.txt
cat logs/last-completion-time.txt

# Browse Spark events and checkpoints
ls spark-events/
ls spark-checkpoints/

# Exit when done
exit
```

### Cleanup Debug Pod

```bash
kubectl delete pod cdm-debug
```

**Note:** The PVC (`cdm-data-pvc`) persists all data until manually deleted, allowing multiple job runs to resume from checkpoints and maintain historical logs.

## Troubleshooting

### Common Issues

1. **Service Discovery**: Ensure service DNS names are correct
   ```bash
   kubectl get svc -n <your-namespace> | grep contact-points
   ```

2. **Authentication**: Verify superuser credentials
   ```bash
   kubectl get secret <cluster-name>-superuser -o yaml
   ```

3. **Network Connectivity**: Test pod-to-pod communication
   ```bash
   kubectl exec -it <pod-name> -- nslookup <service-name>
   ```

### Migration Monitoring

Check job status and logs:
```bash
kubectl get jobs -n <your-namespace>
kubectl describe job cassandra-data-migrator -n <your-namespace>
kubectl logs job/cassandra-data-migrator -n <your-namespace>
```

## Customization

### Migrating Different Tables

To migrate a different table:

1. Update `spark.cdm.schema.origin.keyspaceTable` in `cdm-config/cdm-migration.yaml`
2. Ensure the target keyspace and table exist
3. Adjust performance settings if needed

### Performance Tuning

For large datasets, consider adjusting:
- `spark.cdm.perfops.batchSize`: Increase for better throughput
- `spark.cdm.perfops.numSplits`: Match to available CPU cores
- Job resource limits: Scale based on data volume

## Best Practices

- Always test migrations in non-production environments first
- Verify data integrity post-migration using row counts and checksums
- Monitor resource usage during migration
- Use appropriate batch sizes based on row complexity
- Ensure sufficient disk space for temporary files

## Zero Downtime Migration (ZDM) Proxy

The `zdm-proxy/` directory contains configurations for DataStax's Zero Downtime Migration proxy, which allows you to migrate data from your origin cluster to the target cluster while maintaining read and write operations with minimal downtime.

### ZDM Proxy Components

| File | Purpose | Description |
|------|---------|-------------|
| `zdm-proxy.yaml` | Proxy Deployment | ZDM proxy deployment, service, and pod disruption budget |
| `fraud-detection-zdm.yaml` | Performance Testing | NoSQLBench workload for testing proxy with read/write operations |

### ZDM Proxy Setup

1. **Deploy the ZDM Proxy**:
   ```bash
   kubectl apply -f zdm-proxy/zdm-proxy.yaml
   ```

2. **Verify Proxy Status**:
   ```bash
   kubectl get pods -n <your-namespace> -l app=zdm-proxy
   kubectl logs deployment/zdm-proxy -n <your-namespace>
   ```

### Environment Variables Configuration

The ZDM proxy deployment (`zdm-proxy/zdm-proxy.yaml`) includes the following key environment variables that configure the connection to both origin and target clusters:

#### Proxy Configuration
| Variable | Value | Description |
|----------|--------|-------------|
| `ZDM_PROXY_LISTEN_ADDRESS` | `0.0.0.0` | Proxy listen interface |
| `ZDM_PROXY_LISTEN_PORT` | `14002` | Proxy listen port |
| `ZDM_PRIMARY_CLUSTER` | `ORIGIN` | Primary cluster designation |
| `ZDM_READ_MODE` | `PRIMARY_ONLY` | Read routing mode |
| `ZDM_LOG_LEVEL` | `INFO` | Logging level |

#### Origin Cluster (Apache Cassandra)
| Variable | Value | Description |
|----------|--------|-------------|
| `ZDM_ORIGIN_CONTACT_POINTS` | `cassandra-cassandra-dc-1-service.hcd-epk60j1n.svc.cluster.local` | Origin cluster service endpoint |
| `ZDM_ORIGIN_PORT` | `9042` | Origin cluster CQL port |
| `ZDM_ORIGIN_USERNAME` | `cassandra-superuser` | Origin cluster username |
| `ZDM_ORIGIN_PASSWORD` | From secret `cassandra-superuser` | Origin cluster password (from Kubernetes secret) |

#### Target Cluster (DataStax HCD)
| Variable | Value | Description |
|----------|--------|-------------|
| `ZDM_TARGET_CONTACT_POINTS` | `hcd-hcd-dc-1-service.hcd-epk60j1n.svc.cluster.local` | Target cluster service endpoint |
| `ZDM_TARGET_PORT` | `9042` | Target cluster CQL port |
| `ZDM_TARGET_USERNAME` | `hcd-superuser` | Target cluster username |
| `ZDM_TARGET_PASSWORD` | From secret `hcd-superuser` | Target cluster password (from Kubernetes secret) |

**Important:** Update the namespace `hcd-epk60j1n` in the contact points and secret references to match your deployment namespace.

### Testing with NoSQLBench

The `fraud-detection-zdm.yaml` file contains a comprehensive NoSQLBench workload that tests the ZDM proxy with realistic fraud detection scenarios including:

- **Schema Creation**: Creates `fraud_detection` keyspace and tables
- **Data Generation**: Produces realistic transaction data with fraud indicators
- **Read Operations**: Performs transaction lookups and user transaction queries  
- **Write Operations**: Inserts new transactions and user activity records
- **Load Testing**: Runs continuous read/write workloads to test proxy performance

**To run the performance test**:
```bash
kubectl apply -f zdm-proxy/fraud-detection-zdm.yaml
kubectl logs job/fraud-detection-nb -n applications -f
```

This workload connects to the ZDM proxy service and performs mixed read/write operations, allowing you to validate that the proxy correctly routes traffic between your origin and target clusters.

### Client Connection

After deploying the ZDM proxy, clients can connect through the proxy service instead of directly to either cluster:

**Connection Details**:
- **Host**: `zdm-proxy-service.<your-namespace>.svc.cluster.local`
- **Port**: `9042` (standard Cassandra port)
- **Credentials**: Use your target cluster (HCD) credentials

For detailed information on configuring clients to use the ZDM proxy, refer to the [DataStax ZDM documentation](https://docs.datastax.com/en/data-migration/connect-clients-to-proxy.html).

## References

- [DataStax Cassandra Data Migrator](https://github.com/datastax/cassandra-data-migrator)
- [DataStax Mission Control](https://docs.datastax.com/en/mission-control/)
- [DataStax Zero Downtime Migration](https://docs.datastax.com/en/data-migration/connect-clients-to-proxy.html)
- [Apache Cassandra Documentation](https://cassandra.apache.org/doc/) 