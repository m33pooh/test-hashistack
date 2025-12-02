output "nomad_job_status" {
  description = "Status of the deployed Nomad job"
  value       = nomad_job.app.status
}

output "app_addresses" {
  description = "Addresses where the app can be reached"
  value       = "Check Nomad UI at ${var.nomad_addr}/ui/jobs/app"
}