variable "app_count" {
  type = number
}

variable "app_port" {
  type = number
}

variable "image" {
  type = string
}

job "app" {
  datacenters = ["dc1"]
  type        = "service"

  # Enable Nomad's Vault integration (Nomad must be configured to talk to Vault)
  vault {
    policies = ["app-policy"]
  }

  group "app" {
    count = var.app_count

    network {
      port "http" {
        static = var.app_port
      }
    }

    service {
      name = "springboot-app"
      port = "http"

      # Enable Consul Connect sidecar to provide mTLS (service mesh)
      connect {
        sidecar_service {}
      }

      check {
        type     = "http"
        path     = "/actuator/health"
        interval = "10s"
        timeout  = "2s"
      }
    }

    task "server" {
      driver = "docker"

      config {
        # image will be provided via Terraform (set from CI as TF_VAR_app_image)
        image = var.image
        ports = ["http"]
      }

      # Use Nomad templates to render Vault secrets into env vars (requires Nomad+Vault integration)
      template {
        data = <<EOH
{{- with secret "secret/data/myapp" -}}
DB_USER={{ .Data.data.username }}
DB_PASS={{ .Data.data.password }}
{{- end -}}
EOH
        destination = "secrets/env"
        env         = true
        change_mode = "restart"
      }

      resources {
        cpu    = 500
        memory = 256
      }
    }
  }
}
