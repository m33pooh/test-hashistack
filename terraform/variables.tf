variable "app_count" {
  description = "Number of app instances to run"
  type        = number
  default     = 1
}

variable "app_port" {
  description = "Port the app listens on"
  type        = number
  default     = 8080
}

variable "app_image" {
  description = "Container image to run for the app (set via TF_VAR_app_image or CI)"
  type        = string
  default     = "hashistack-springboot-app:latest"
}

variable "nomad_addr" {
  description = "Nomad API address"
  type        = string
  default     = "http://localhost:4646"
}

variable "vault_addr" {
  description = "Vault API address"
  type        = string
  default     = "http://localhost:8200"
}

variable "consul_addr" {
  description = "Consul API address"
  type        = string
  default     = "http://localhost:8500"
}