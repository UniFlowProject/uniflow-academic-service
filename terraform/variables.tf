variable "project-tag" {
  type        = string
  description = "The tag to use for the project"
  default     = "uniflow"
}

variable "aws_region" {
  type        = string
  description = "AWS region for the service stack"
  default     = "us-east-1"
}

variable "container_port" {
  type        = number
  description = "Port the Spring Boot app listens on (matches Dockerfile SERVER_PORT)"
  default     = 3001
}

variable "task_cpu" {
  type        = number
  description = "Fargate task CPU units"
  default     = 512
}

variable "task_memory" {
  type        = number
  description = "Fargate task memory in MiB"
  default     = 1024
}

variable "desired_count" {
  type        = number
  description = "Number of Fargate tasks to run"
  default     = 1
}

variable "image_tag" {
  type        = string
  description = "ECR image tag deployed by the task definition"
  default     = "latest"
}

variable "log_retention_days" {
  type        = number
  description = "CloudWatch log retention for the service"
  default     = 14
}

variable "github_repo" {
  type        = string
  description = "GitHub repo allowed to assume the CI role via OIDC, as org/repo. CHANGE THIS to the real repo before applying."
  default     = "Uniflow/uniflow-academic-service"

  validation {
    condition     = !can(regex("[*?]", var.github_repo))
    error_message = "github_repo must not contain wildcards (* or ?) — the OIDC trust condition uses StringEquals and expects an exact org/repo."
  }
}
