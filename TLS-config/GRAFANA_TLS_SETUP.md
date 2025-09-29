# Grafana TLS Configuration Guide

This guide explains how to enable TLS encryption for the mission-control-grafana pod in the Mission Control deployment.

## Prerequisites

- cert-manager installed in the cluster
- ClusterIssuer `mission-control-selfsigned` configured

## Step 1: Create TLS Certificate

First, create the certificate resource to generate the TLS certificate for Grafana:

Create `grafana-certificate.yaml`:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: grafana-tls-secret
  namespace: mission-control
spec:
  secretName: grafana-tls-secret  
  issuerRef:
    name: mission-control-selfsigned              
    kind: ClusterIssuer
  commonName: mc-grafana.mission-control.svc.cluster.local  
  dnsNames:
    - mission-control-grafana.mission-control.svc.cluster.local                  
  usages:
    - digital signature
    - key encipherment
```

Apply the certificate:
```bash
kubectl apply -f grafana-certificate.yaml
```

## Step 2: Configure Grafana for HTTPS

Add the following configuration to your Helm values file (`mission-control-values/override.yaml`):

```yaml
grafana:
  enabled: true
  grafana.ini:
    server:
      protocol: https
      http_port: 3000
      cert_file: /etc/grafana/ssl/tls.crt
      cert_key: /etc/grafana/ssl/tls.key
  extraSecretMounts:
    - name: grafana-tls
      mountPath: /etc/grafana/ssl
      secretName: grafana-tls-secret
      readOnly: true
  readinessProbe:
    httpGet:
      scheme: HTTPS
      path: /api/health
      port: 3000
  livenessProbe:
    httpGet:
      scheme: HTTPS
      path: /api/health
      port: 3000
```

## Step 3: Apply Configuration

Deploy the changes using Helm:

```bash
helm upgrade mission-control oci://registry.replicated.com/mission-control/mission-control -f ./mission-control-values/override.yaml -n mission-control
```

## Key Configuration Elements

- **protocol: https**: Configures Grafana server to use HTTPS
- **cert_file/cert_key**: Points to the TLS certificate and key files
- **extraSecretMounts**: Mounts the TLS secret into the pod at `/etc/grafana/ssl`
- **readinessProbe/livenessProbe**: Updated to use HTTPS scheme to prevent 400 errors

## Accessing Grafana

After TLS is enabled, access Grafana using port-forward:

```bash
kubectl port-forward svc/mission-control-grafana 3000:3000 -n mission-control
```

Then navigate to `https://localhost:3000`

**Default Credentials:**
- Username: `admin`
- Password: Retrieved from secret (use: `kubectl get secret mission-control-grafana -n mission-control -o jsonpath='{.data.admin-password}' | base64 -d`)

## Troubleshooting

If you encounter readiness probe failures with 400 status codes, ensure that:
1. The certificate secret exists and contains valid TLS data
2. The readiness and liveness probes are configured to use HTTPS scheme
3. The certificate DNS names match the service endpoints