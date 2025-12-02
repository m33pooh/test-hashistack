Simple Spring Boot example that demonstrates basic interaction with the HashiStack (Consul + Vault) in the repository's Docker Compose stack.

How it works:
- Exposes GET /hello on port 8080.
- When called, the app will attempt to read services from Consul at `http://consul:8500` and try to read `secret/data/myapp` from Vault at `http://vault:8200` using the `VAULT_TOKEN` env var (defaults to `root`).

Build & run with your existing compose setup (from repo root):

```powershell
# build only the springboot-app image
docker compose build springboot-app

# start the stack and the app (compose will use existing services from docker-compose.yml)
docker compose up -d consul vault nomad springboot-app

# check the app
Invoke-WebRequest -UseBasicParsing http://localhost:8080/hello | Select-Object -ExpandProperty Content
```

Notes:
- Vault in this repo is running in dev mode with token `root`. Add any secret under `secret/data/myapp` if you want the app to return a stored secret.
- The app uses simple HTTP calls to Consul and Vault so there are no Spring Cloud dependencies required.
