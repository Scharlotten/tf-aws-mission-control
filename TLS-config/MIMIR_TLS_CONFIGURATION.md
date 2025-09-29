# Mimir TLS Configuration Documentation

This document explains the TLS configuration implemented for all Mimir components in the mission-control deployment. The configuration enables secure communication between all Mimir components using mutual TLS (mTLS).

## Prerequisites: Certificate Management

Before any TLS configuration can work, a Certificate CRD object must be created to generate and mount the TLS secrets to each pod. This is handled by the `mimir-tls-certificate.yaml` file:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: mimir-tls-secret
  namespace: mission-control
spec:
  secretName: mimir-tls-secret  
  issuerRef:
    name: mission-control-selfsigned              
    kind: ClusterIssuer
  commonName: mimir-query-scheduler.mission-control.svc.cluster.local  
  dnsNames:
    - mission-control-mimir-query-scheduler.mission-control.svc.cluster.local
    - mission-control-mimir-query-frontend.mission-control.svc.cluster.local
    - mission-control-mimir-querier.mission-control.svc.cluster.local
    - mission-control-mimir-distributor.mission-control.svc.cluster.local
    - mission-control-mimir-ingester.mission-control.svc.cluster.local
    - mission-control-mimir-store-gateway.mission-control.svc.cluster.local
    - mission-control-mimir-compactor.mission-control.svc.cluster.local
    - mission-control-mimir-ruler.mission-control.svc.cluster.local
    - mission-control-mimir-alertmanager.mission-control.svc.cluster.local
    - mission-control-mimir-overrides-exporter.mission-control.svc.cluster.local
  usages:
    - digital signature
    - key encipherment
    - server auth
    - client auth
```

This certificate generates a Kubernetes secret (`mimir-tls-secret`) containing:
- `tls.crt` - The TLS certificate
- `tls.key` - The private key
- `ca.crt` - The certificate authority certificate

## Global TLS Configuration

All Mimir components share a common TLS volume configuration that mounts the certificate secret:

```yaml
mimir:
  global:
    extraVolumes:
      - name: mimir-tls
        secret:
          secretName: mimir-tls-secret
    extraVolumeMounts:
      - name: mimir-tls
        mountPath: /etc/mimir/tls
        readOnly: true
```

This configuration ensures that all pods have access to the TLS certificates at `/etc/mimir/tls/`.

## Component-Specific TLS Configuration

### 1. Query Scheduler

The query scheduler acts as a central coordination point and requires TLS for:
- **Server-side TLS**: Accepting gRPC connections from queriers
- **Client-side TLS**: Connecting to etcd for ring management

```yaml
query_scheduler:
  extraArgs:
    # Server TLS configuration
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    
    # Client TLS for gRPC connections
    query-scheduler.grpc-client-config.tls-enabled: "true"
    query-scheduler.grpc-client-config.tls-cert-path: /etc/mimir/tls/tls.crt
    query-scheduler.grpc-client-config.tls-key-path: /etc/mimir/tls/tls.key
    query-scheduler.grpc-client-config.tls-insecure-skip-verify: "true"
    
    # Etcd ring TLS configuration
    query-scheduler.ring.etcd.tls-enabled: "true"
    query-scheduler.ring.etcd.tls-cert-path: /etc/mimir/tls/tls.crt
    query-scheduler.ring.etcd.tls-key-path: /etc/mimir/tls/tls.key
    query-scheduler.ring.etcd.tls-ca-path: /etc/mimir/tls/ca.crt
    query-scheduler.ring.etcd.tls-insecure-skip-verify: "true"
```

### 2. Query Frontend

The query frontend handles query requests and communicates with query schedulers:

```yaml
query_frontend:
  extraArgs:
    # Server TLS configuration
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    
    # Client TLS for gRPC connections
    query-frontend.grpc-client-config.tls-enabled: "true"
    query-frontend.grpc-client-config.tls-cert-path: /etc/mimir/tls/tls.crt
    query-frontend.grpc-client-config.tls-key-path: /etc/mimir/tls/tls.key
    query-frontend.grpc-client-config.tls-insecure-skip-verify: "true"
```

### 3. Querier

The querier has the most complex TLS configuration as it communicates with multiple components:

```yaml
querier:
  extraArgs:
    # Server TLS configuration
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    
    # Frontend client TLS
    querier.frontend-client.tls-enabled: "true"
    querier.frontend-client.tls-cert-path: /etc/mimir/tls/tls.crt
    querier.frontend-client.tls-key-path: /etc/mimir/tls/tls.key
    querier.frontend-client.tls-insecure-skip-verify: "true"
    
    # Store-gateway client TLS
    querier.store-gateway-client.tls-enabled: "true"
    querier.store-gateway-client.tls-cert-path: /etc/mimir/tls/tls.crt
    querier.store-gateway-client.tls-key-path: /etc/mimir/tls/tls.key
    querier.store-gateway-client.tls-ca-path: /etc/mimir/tls/ca.crt
    querier.store-gateway-client.tls-insecure-skip-verify: "true"
    
    # Scheduler client TLS
    querier.scheduler-client.tls-enabled: "true"
    querier.scheduler-client.tls-cert-path: /etc/mimir/tls/tls.crt
    querier.scheduler-client.tls-key-path: /etc/mimir/tls/tls.key
    querier.scheduler-client.tls-ca-path: /etc/mimir/tls/ca.crt
    querier.scheduler-client.tls-insecure-skip-verify: "true"
    
    # Ingester client TLS
    ingester.client.tls-enabled: "true"
    ingester.client.tls-cert-path: /etc/mimir/tls/tls.crt
    ingester.client.tls-key-path: /etc/mimir/tls/tls.key
    ingester.client.tls-ca-path: /etc/mimir/tls/ca.crt
    ingester.client.tls-insecure-skip-verify: "true"
```

### 4. Ruler

The ruler evaluates recording and alerting rules:

```yaml
ruler:
  extraArgs:
    # Server TLS configuration
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    
    # Query frontend client TLS
    ruler.query-frontend.grpc-client-config.tls-enabled: "true"
    ruler.query-frontend.grpc-client-config.tls-cert-path: /etc/mimir/tls/tls.crt
    ruler.query-frontend.grpc-client-config.tls-key-path: /etc/mimir/tls/tls.key
    ruler.query-frontend.grpc-client-config.tls-insecure-skip-verify: "true"
```

### 5. Alertmanager

The alertmanager handles alert notifications and requires TLS for clustering:

```yaml
alertmanager:
  extraArgs:
    # Server TLS configuration with client certificate requirement
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    server.grpc-tls-client-auth: RequireClientCert
    server.grpc-tls-ca-path: /etc/mimir/tls/ca.crt
    
    # Sharding ring etcd TLS
    alertmanager.sharding-ring.etcd.tls-enabled: "true"
    alertmanager.sharding-ring.etcd.tls-cert-path: /etc/mimir/tls/tls.crt
    alertmanager.sharding-ring.etcd.tls-key-path: /etc/mimir/tls/tls.key
    alertmanager.sharding-ring.etcd.tls-ca-path: /etc/mimir/tls/ca.crt
    alertmanager.sharding-ring.etcd.tls-insecure-skip-verify: "true"
```

### 6. Ingester

The ingester stores time series data and participates in multiple rings:

```yaml
ingester:
  extraArgs:
    # Server TLS configuration with client certificate requirement
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    server.grpc-tls-client-auth: RequireClientCert
    server.grpc-tls-ca-path: /etc/mimir/tls/ca.crt
    
    # Ingester ring etcd TLS
    ingester.ring.etcd.tls-enabled: "true"
    ingester.ring.etcd.tls-cert-path: /etc/mimir/tls/tls.crt
    ingester.ring.etcd.tls-key-path: /etc/mimir/tls/tls.key
    ingester.ring.etcd.tls-ca-path: /etc/mimir/tls/ca.crt
    ingester.ring.etcd.tls-insecure-skip-verify: "true"
    
    # Ingester client TLS for peer communication
    ingester.client.tls-enabled: "true"
    ingester.client.tls-cert-path: /etc/mimir/tls/tls.crt
    ingester.client.tls-key-path: /etc/mimir/tls/tls.key
    ingester.client.tls-ca-path: /etc/mimir/tls/ca.crt
    ingester.client.tls-insecure-skip-verify: "true"
    
    # Partition ring etcd TLS
    ingester.partition-ring.etcd.tls-enabled: "true"
    ingester.partition-ring.etcd.tls-cert-path: /etc/mimir/tls/tls.crt
    ingester.partition-ring.etcd.tls-key-path: /etc/mimir/tls/tls.key
    ingester.partition-ring.etcd.tls-ca-path: /etc/mimir/tls/ca.crt
    ingester.partition-ring.etcd.tls-insecure-skip-verify: "true"
```

### 7. Store Gateway

The store gateway provides access to historical data:

```yaml
store_gateway:
  extraArgs:
    # Server TLS configuration with client certificate requirement
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    server.grpc-tls-client-auth: RequireClientCert
    server.grpc-tls-ca-path: /etc/mimir/tls/ca.crt
```

### 8. Compactor

The compactor compacts and cleans up old blocks:

```yaml
compactor:
  extraArgs:
    # Server TLS configuration with client certificate requirement
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    server.grpc-tls-client-auth: RequireClientCert
    server.grpc-tls-ca-path: /etc/mimir/tls/ca.crt
```

### 9. Distributor

The distributor receives incoming samples and distributes them to ingesters:

```yaml
distributor:
  extraArgs:
    # Server TLS configuration with client certificate requirement
    server.grpc-tls-cert-path: /etc/mimir/tls/tls.crt
    server.grpc-tls-key-path: /etc/mimir/tls/tls.key
    server.grpc-tls-client-auth: RequireClientCert
    server.grpc-tls-ca-path: /etc/mimir/tls/ca.crt
    
    # HA tracker etcd TLS
    distributor.ha-tracker.etcd.tls-enabled: "true"
    distributor.ha-tracker.etcd.tls-cert-path: /etc/mimir/tls/tls.crt
    distributor.ha-tracker.etcd.tls-key-path: /etc/mimir/tls/tls.key
    distributor.ha-tracker.etcd.tls-ca-path: /etc/mimir/tls/ca.crt
    distributor.ha-tracker.etcd.tls-insecure-skip-verify: "true"
```

## TLS Configuration Patterns

### Common Parameters

All TLS configurations use these common patterns:

- **`tls-enabled: "true"`** - Enables TLS for the specific connection type
- **`tls-cert-path`** - Path to the client/server certificate
- **`tls-key-path`** - Path to the private key
- **`tls-ca-path`** - Path to the CA certificate for validation
- **`tls-insecure-skip-verify: "true"`** - Skip certificate validation (acceptable for self-signed certificates in controlled environments)

### Server vs Client TLS

- **Server TLS**: Configured with `server.grpc-tls-*` parameters for components accepting incoming connections
- **Client TLS**: Configured with component-specific client parameters (e.g., `querier.frontend-client.tls-*`) for outgoing connections

## Further Documentation

For comprehensive information about Mimir TLS configuration, refer to the official Grafana documentation:
[Securing Communications with TLS](https://grafana.com/docs/mimir/latest/manage/secure/securing-communications-with-tls/)


