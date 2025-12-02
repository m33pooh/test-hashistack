CI / Deployment README

What this pipeline does

- build: Runs `mvn package` and stores the artifact.
- publish: Builds a Docker image for `springboot-app` and pushes it to the container registry (uses `CI_REGISTRY` / `CI_REGISTRY_IMAGE`).
- deploy: Runs Terraform to create/refresh a Vault secret and submit a Nomad job which registers a Consul service.

Required GitLab CI/CD variables (set in project Settings → CI/CD → Variables)

- CI_REGISTRY_USER / CI_REGISTRY_PASSWORD: Provided automatically for GitLab Container Registry; else set credentials for your registry.
- NOMAD_ADDR: URL to Nomad API (e.g. http://nomad.example:4646)
- VAULT_ADDR: URL to Vault API (e.g. http://vault.example:8200)
- CONSUL_HTTP_ADDR: URL to Consul (e.g. http://consul.example:8500)

Runner / Networking considerations

- The `terraform_deploy` job must be able to reach Nomad, Vault and Consul. There are two common options:
  1) Use a self-hosted GitLab Runner on the same network (or host) where Nomad/Consul/Vault run. This is simplest and recommended for local stacks.
  2) Use Docker-in-Docker and connect the runner to the host network or a VPN that can reach the services. This is more advanced and may require privileged runners.

AppRole / Nomad Vault integration notes

- The Terraform configuration now creates a Vault policy (`app-policy`) and an AppRole (`app-role`). The pipeline outputs `approle_role_id` and `approle_secret_id` from Terraform when you run it. Use these values carefully — `approle_secret_id` is sensitive.
- For the most secure Nomad integration you should configure Nomad's built-in Vault integration (Nomad mints short-lived tokens for tasks) and add `vault { policies = ["app-policy"] }` to your Nomad job (already present in `nomad/app.nomad.hcl`). This avoids distributing AppRole credentials to the task.
- If you prefer AppRole-based authentication (app authenticates directly to Vault), use the AppRole `role_id` and `secret_id` created by Terraform; store them securely (for example in a Vault Agent or Consul Template) and avoid hardcoding them into images or repo.

Consul Connect (mTLS)

- The Nomad job enables Consul Connect sidecar (`connect { sidecar_service {} }`) so services will get mTLS-secured connections when Consul Connect is properly enabled in your Consul cluster.
- Ensure Consul's Connect CA is enabled and Nomad is configured to register services with Connect enabled.

Database secrets (dynamic credentials)

- Terraform now includes resources to enable Vault's Database secrets engine and create a PostgreSQL connection and role. The role `app-db-role` issues dynamic credentials at the Vault endpoint `database/creds/app-db-role`.
- For local testing the repo includes a `postgres` service in `docker-compose.yml`. The demo uses the default `postgres` user and password (`postgres`) and DB `appdb`. In production, configure Vault to talk to your managed DB and secure the bootstrap credentials.
- To obtain dynamic credentials manually (once Terraform has applied and Vault DB plugin/connection works):

  ```powershell
  # Example: read dynamic DB creds from Vault
  curl --header "X-Vault-Token: <token>" http://vault:8200/v1/database/creds/app-db-role
  ```

  The response will contain `username` and `password` that are valid for a short TTL.

Security notes

- Do NOT store Vault root tokens in the repo. Use CI variables and RBAC.
- For production, configure Nomad's Vault integration rather than embedding tokens in job env.

How the Nomad job registers with Consul

- The Nomad job (nomad/app.nomad.hcl) contains a `service` stanza; when Nomad runs the task the service is automatically registered in Consul.
- If you prefer manual registration, `springboot-app/consul-service.json` provides an example JSON you can POST to the Consul agent: `curl --request PUT --data @consul-service.json http://consul:8500/v1/agent/service/register`

Local testing

- You can test the pipeline steps locally with the included `deploy.ps1` script which:
  - Builds the Docker image locally
  - Starts Consul and Vault via Docker Compose
  - Runs Terraform in a containerized environment (requires docker-compose/runner to be able to reach services)

If you'd like, I can:
- Update `.gitlab-ci.yml` to also run integration tests (start stack in a runner and run the app endpoint).
- Provide an example `terraform.tfvars` for non-local deployments.
- Add instructions to run Nomad as an install on the host instead of inside Docker (recommended for stability).
