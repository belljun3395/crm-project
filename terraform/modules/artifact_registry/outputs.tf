output "repository_id" {
  description = "Artifact Registry repository ID."
  value       = google_artifact_registry_repository.this.repository_id
}

output "repository_url" {
  description = "URL used to push artifacts (for Docker: <location>-docker.pkg.dev/<project>/<repo>)."
  value       = local.effective_repository_url
}
