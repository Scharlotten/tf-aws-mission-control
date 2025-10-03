
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

| File | Purpose | Description |
|------|---------|-------------|
| `cassandra.yaml` | Source Cluster | Apache Cassandra 3.11.15 MissionControlCluster manifest |
| `hcd.yaml` | Target Cluster | DataStax HCD 1.2.3 MissionControlCluster manifest |
| `cdm-migration.yaml` | Migration Job | CDM configuration and Kubernetes Job for data migration |
| `cdm-migration-job-with-pvc.yaml` | Migration Job with Persistence | CDM Job with PersistentVolume for state and log persistence |
| `cdm-job-pvc-pickup.yaml` | Debug Pod | Ubuntu debug pod to access PVC logs and data after job completion |
| `query-cassandra-java/` | Testing Tool | Java application for database connectivity testing |

## Quick Start

### 1. Deploy Database Clusters

**Important:** Update the `namespace` field in both manifests to match your environment.

Deploy the source Apache Cassandra cluster:
```bash
kubectl apply -f cassandra.yaml
```

Deploy the target HCD cluster:
```bash
kubectl apply -f hcd.yaml
```

### 2. Verify Cluster Deployment

Check cluster status:
```bash
kubectl get missioncontrolcluster -n <your-namespace>
kubectl get pods -n <your-namespace>
```

Wait for all pods to be in `Running` state before proceeding.

### 3. Configure Migration Parameters

Edit `cdm-migration.yaml` and update the following:

- **Namespace**: Change `namespace: hcd-epk60j1n` to your namespace
- **Passwords**: Replace `yourPassword` with actual superuser passwords
- **Service Names**: Verify service DNS names match your deployment:
  ```bash
  kubectl get svc -n <your-namespace>
  ```

### 4. Execute Migration

**Option A: Basic Job (No Persistence)**
```bash
kubectl apply -f cdm-migration.yaml
kubectl logs -f job/cassandra-data-migrator -n <your-namespace>
```

**Option B: Job with Persistent Volume (Recommended for Large Migrations)**
```bash
# Deploy job with PVC for state persistence and resumability
kubectl apply -f cdm-migration-job-with-pvc.yaml
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
kubectl apply -f cdm-job-pvc-pickup.yaml
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

1. Update `spark.cdm.schema.origin.keyspaceTable` in `cdm-migration.yaml`
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

## References

- [DataStax Cassandra Data Migrator](https://github.com/datastax/cassandra-data-migrator)
- [DataStax Mission Control](https://docs.datastax.com/en/mission-control/)
- [Apache Cassandra Documentation](https://cassandra.apache.org/doc/) 