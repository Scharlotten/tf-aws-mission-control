# DataStax Mission Control on Amazon EKS
## Complete Deployment Guide

Deploy DataStax Mission Control on a fully managed Amazon EKS cluster using Terraform automation. This guide walks you through setting up the infrastructure, configuring authentication, and deploying Mission Control for production-ready Cassandra operations.

---

## 🚀 Quick Start Overview

This deployment creates:
- **Amazon EKS cluster** with optimized node groups
- **DataStax Mission Control** for Cassandra management
- **Monitoring stack** with Loki for centralized logging
- **Security configurations** following AWS best practices

**Estimated deployment time:** 15-20 minutes

---

## 📋 Prerequisites

### Required Tools Installation

Ensure these tools are installed and configured on your local machine:

| Tool | Purpose | Installation Link |
|------|---------|------------------|
| **AWS CLI** | AWS authentication and resource management | [Install AWS CLI](https://aws.amazon.com/cli/) |
| **kubectl** | Kubernetes cluster interaction | [Install kubectl](https://kubernetes.io/docs/tasks/tools/) |
| **Terraform** | Infrastructure as Code deployment | [Install Terraform](https://developer.hashicorp.com/terraform/install) |

### Verification Commands
```bash
# Verify installations
aws --version
kubectl version --client
terraform --version
```

---

## 🔐 AWS Authentication Setup

### Configure AWS SSO Profile

1. **Locate your AWS configuration directory:**
   - **Windows:** `C:\Users\<username>\.aws`
   - **Linux/macOS:** `~/.aws`

2. **Create configuration file** (`~/.aws/config`):
```ini
[profile default]
sso_start_url = <your_sso_start_url>
sso_region = us-west-2
sso_account_id = <your_account_id>
sso_role_name = FIELDOPS_FOPS-PRE
```

### Finding Your SSO Details

**SSO Start URL:**
- Access through Okta: Click "AWS SSO" 
- Format: `https://d-<sso_id>.awsapps.com/start`

**Account ID:**
- Login to AWS Console
- Click your username (top-right corner)
- Copy the Account ID displayed

![AWS Account ID Location](image.png)

### Authenticate to AWS
```bash
aws sso login
```

---

## ⚙️ Configuration

### Update Variables File

Configure the following parameters in your `terraform.tfvars` file:

```hcl
# Infrastructure Configuration
region       = "us-west-2"              # AWS region for deployment
cluster_name = "my-mission-control-eks"  # EKS cluster name

# Storage Configuration  
loki_bucket = "my-company-loki-logs"     # S3 bucket for Loki logs

# DataStax Mission Control
user_email  = "admin@company.com"        # Registry login email
license_id  = "your-license-id-here"     # Mission Control license

# Resource Tagging
username = "your-username"               # Resource ownership tag

# Compute Configuration
instance_type_db = "m5.large"            # Database node instance type
```

### Obtain DataStax License

1. Visit the [DataStax Mission Control download page](https://www.datastax.com/products/mission-control/download)
2. Sign up for a **free community license**
3. Copy your license ID to the configuration file

---

## 🚀 Deployment Process

### 1. Initialize Terraform
```bash
terraform init
```
This downloads required provider plugins and modules.

### 2. Plan Deployment
```bash
terraform plan -out=deployment.plan
```

**Common Issues:**
- **VPC Limit Exceeded:** AWS accounts have a default limit of 5 VPCs per region
  - **Solution 1:** Choose a different region with available VPC capacity
  - **Solution 2:** Request VPC limit increase through AWS Support

### 3. Apply Configuration
```bash
terraform apply deployment.plan
```

### 4. Configure kubectl
```bash
aws eks update-kubeconfig --region <your-region> --name <cluster-name>
```

---

## 📦 Mission Control Installation

### Standard Environment Deployment

For **non-airgapped environments**, configure the Helm installation:

1. **Update the `helm_override_file` parameter** in your variables file
2. **Reference provided examples:**
   - Standard deployment: Use default configuration
   - Airgapped deployment: See `airgapped.yaml` example

### Airgapped Environment Setup

For environments without internet access, additional configuration is required:

```yaml
# Example airgapped.yaml configuration
image:
  registry: "your-private-registry.com"
  pullPolicy: "IfNotPresent"

security:
  networkPolicies:
    enabled: true
```

---

## ✅ Post-Deployment Verification

### Check Cluster Status
```bash
kubectl get nodes
kubectl get pods --all-namespaces
```

### Access Mission Control
1. Retrieve the service endpoint:
```bash
kubectl get services -n mission-control
```

2. Access the web interface using the provided LoadBalancer URL

### Verify Monitoring Stack
```bash
kubectl get pods -n monitoring
```

---

## 🛠️ Troubleshooting

### Common Issues and Solutions

**Authentication Failures:**
- Verify AWS SSO configuration
- Check IAM permissions for EKS operations

**Resource Limits:**
- Monitor VPC limits in target region
- Verify EC2 instance quotas

**Networking Issues:**
- Confirm security group configurations
- Check subnet CIDR ranges for conflicts

### Support Resources

- **DataStax Documentation:** [Mission Control Docs](https://docs.datastax.com/)
- **AWS EKS Troubleshooting:** [EKS User Guide](https://docs.aws.amazon.com/eks/)
- **Terraform AWS Provider:** [Provider Documentation](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

---

## 🧹 Cleanup

To remove all deployed resources:
```bash
terraform destroy
```

**Warning:** This will permanently delete your EKS cluster and all associated resources. Ensure you have backed up any important data before proceeding.

---

## 📝 Next Steps

After successful deployment:

1. **Configure Mission Control** for your Cassandra clusters
2. **Set up monitoring dashboards** using the deployed Loki stack  
3. **Configure backup strategies** for your databases
4. **Review security settings** and implement additional hardening as needed

For advanced configuration options and production optimization, consult the DataStax Mission Control documentation.