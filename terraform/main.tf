# Configure providers
terraform {
  required_providers {
    nomad = {
      source = "hashicorp/nomad"
    }
    vault = {
      source = "hashicorp/vault"
    }
    consul = {
      source = "hashicorp/consul"
    }
  }
}

provider "nomad" {
  address = var.nomad_addr
}

provider "vault" {
  address = var.vault_addr
  # Token is set via VAULT_TOKEN env var in docker-compose
}

provider "consul" {
  address = var.consul_addr
}

# Create a Vault secret for the app
resource "vault_kv_secret_v2" "app_secret" {
  mount = "secret"  # KV v2 secrets engine mount point
  name  = "myapp"   # Path our app looks for: secret/data/myapp
  
  data_json = jsonencode({
    username = "appuser",
    password = "s3cr3tP@ssw0rd"
  })
}

# Create a Vault Policy that allows reading the app secret
resource "vault_policy" "app_policy" {
  name   = "app-policy"
  policy = <<EOF
path "secret/data/myapp" {
  capabilities = ["read"]
}
EOF
}

# Enable the AppRole auth backend and create a role for the app
resource "vault_auth_backend" "approle" {
  type = "approle"
}

resource "vault_approle_auth_backend_role" "app_role" {
  backend        = vault_auth_backend.approle.path
  role_name      = "app-role"
  bind_secret_id = true
  token_policies = [vault_policy.app_policy.name]
  token_ttl      = 3600
  token_max_ttl  = 14400
}

# Create a Secret ID for the AppRole (this will produce a secret_id that can be used by the application)
resource "vault_approle_auth_backend_role_secret_id" "app_secret_id" {
  backend   = vault_auth_backend.approle.path
  role_name = vault_approle_auth_backend_role.app_role.role_name
}

output "approle_role_id" {
  value = vault_approle_auth_backend_role.app_role.role_id
}

output "approle_secret_id" {
  value     = vault_approle_auth_backend_role_secret_id.app_secret_id.secret_id
  sensitive = true
}

# Mount and configure the Database secrets engine for dynamic DB credentials
resource "vault_mount" "database" {
  path        = "database"
  type        = "database"
  description = "Database secrets engine for dynamic DB creds"
}

# Configure a PostgreSQL connection for Vault to talk to the DB (adjust connection details for your env)
resource "vault_database_secret_backend_connection" "postgres" {
  backend     = vault_mount.database.path
  name        = "postgres"
  plugin_name = "postgresql-database-plugin"
  allowed_roles = ["app-db-role"]

  postgresql {
    connection_url = "postgresql://{{username}}:{{password}}@postgres:5432/appdb?sslmode=disable"
    username      = "postgres"
    password      = "postgres"
  }
}

# Create a role that issues dynamic credentials for the app
resource "vault_database_secret_backend_role" "app_db_role" {
  backend             = vault_mount.database.path
  name                = "app-db-role"
  db_name             = vault_database_secret_backend_connection.postgres.name
  creation_statements = [
    "CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";"
  ]
  default_ttl = 3600
  max_ttl     = 86400
}

output "vault_db_role_name" {
  value = vault_database_secret_backend_role.app_db_role.name
}

# Deploy app via Nomad
resource "nomad_job" "app" {
  # job file is now included in the terraform module
  jobspec = file("${path.module}/app.nomad.hcl")
  
  # Trigger redeployment when Vault secret changes
  hcl2 {
    vars = {
      app_count = var.app_count
      app_port  = var.app_port
      image     = var.app_image
    }
  }

  depends_on = [vault_kv_secret_v2.app_secret]
}