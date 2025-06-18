provider "helm" {
  kubernetes {
    #config_path = "~/.kube/config"
    #config_path = local.kubeconfig
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)

    # Configure authentication using AWS CLI for EKS cluster
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      args        = ["eks", "get-token", "--cluster-name", var.cluster_name]
      command     = "aws"
    }
    
  }

  # localhost registry with password protection
  registry {
    url = "oci://registry.replicated.com"
    username = var.user_email
    password = var.license_id
  }
}

resource "helm_release" "cert_manager" {
  name       = "cert-manager"
  repository = "https://charts.jetstack.io"
  chart      = "cert-manager"
  version    = "v1.16.2"
  create_namespace = true
  namespace = "cert-manager"
  force_update = true

  set {
    name  = "crds.enabled"
    value = "true"
  }
}


resource "helm_release" "mission_control_datastax" {
  name        = "mission-control"
  namespace   = "mission-control"
  repository  = "oci://registry.replicated.com/mission-control"
  #version     = "1.9.0"
  chart       = "mission-control"
  create_namespace = true
  repository_username = var.user_email
  repository_password = var.license_id
  #repository = "./mc-mimir-updated"
  values = [

    templatefile(var.helm_override_file, { loki_bucket = "${var.loki_bucket}"
                                      mimir_bucket =  "${var.mimir_bucket}"
                                      region = "${var.region}"})
    
  ]
  depends_on = [helm_release.cert_manager]
}


resource "helm_release" "pulsar_datastax" {
  name        = "pulsar"
  namespace   = "kaap"
  repository  = "https://datastax.github.io/kaap"
  chart       = "kaap-stack"
  create_namespace = true

  values = [

    templatefile("./pulsar_values.yaml", {})
    
  ]
  depends_on = [helm_release.cert_manager]
}



resource "kubernetes_namespace" "kubernetes_dashboard" {
  metadata {
    name = "kubernetes-dashboard"
  }
}


###
# Install the Metrics Server using the Helm provider
###
resource "helm_release" "metrics_server" {
  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  namespace  = kubernetes_namespace.kubernetes_dashboard.metadata[0].name
  # Recent updates to the Metrics Server do not work with self-signed certificates by default.
  # Since Docker For Desktop uses such certificates, you’ll need to allow insecure TLS
  set {
    name  = "args"
    value = "{--kubelet-insecure-tls=true}"
  }
  wait = true
}

# Output metadata of the Metrics Server release
output "metrics_server_service_metadata" {
  value = helm_release.metrics_server.metadata
}

