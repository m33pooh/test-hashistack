# Build the Spring Boot app image first
Write-Host "Building springboot-app Docker image..."
docker compose build springboot-app

# Start the HashiStack (Consul, Vault, Nomad)
Write-Host "Starting HashiStack services..."
docker compose up -d consul vault nomad

# Wait for services to be ready
Write-Host "Waiting for services to be ready..."
Start-Sleep -Seconds 10

# Run Terraform to deploy the app via Nomad
Write-Host "Running Terraform to deploy app..."
docker compose run --rm terraform init
docker compose run --rm terraform apply -auto-approve

Write-Host "`nDeployment complete! You can check:"
Write-Host "- Nomad UI:    http://localhost:4646/ui"
Write-Host "- Consul UI:   http://localhost:8500/ui"
Write-Host "- Vault UI:    http://localhost:8200/ui"
Write-Host "- App Endpoint: http://localhost:8080/hello"