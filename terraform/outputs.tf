output "ecr_repository_url" {
  value = aws_ecr_repository.uniflow_ecr.repository_url
}

output "ecs_cluster_name" {
  value = nonsensitive(element(split("/", local.platform.ecs_cluster_arn), 1))
}

output "ecs_service_name" {
  value = aws_ecs_service.uniflow_service.name
}

output "github_actions_role_arn" {
  description = "Set this as the AWS_ROLE_ARN repo variable in GitHub Actions"
  value       = aws_iam_role.github_actions.arn
}
