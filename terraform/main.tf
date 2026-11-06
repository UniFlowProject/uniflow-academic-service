# --- Platform contract: everything below is published by the root stack
# (`terraform/` at the repo root) as /<project>/<name> SSM parameters. This
# stack never reads the root's state and never owns the VPC, the ECS cluster,
# the database or Cognito. Apply the root stack first. ---

locals {
  ssm_names = [
    "vpc_id",
    "public_subnet_ids",
    "ecs_cluster_arn",
    "alb_sg_id",
    "academic_target_group_arn",
    "rds_sg_id",
    "rds_address",
    "rds_port",
    "rds_db_name",
    "rds_secret_arn",
    "cognito_issuer_uri",
    "cognito_client_id",
    "site_url",
    "scheduler_group_name",
    "scheduler_role_arn",
    "expirations_queue_arn",
  ]
}

data "aws_ssm_parameter" "platform" {
  for_each = toset(local.ssm_names)
  name     = "/${var.project-tag}/${each.key}"
}

locals {
  platform = { for k, p in data.aws_ssm_parameter.platform : k => p.value }

  # Demo network: tasks run in the public subnets with a public IP (no NAT
  # gateway). They are still unreachable from the internet: the security group
  # only admits the internal ALB.
  service_subnet_ids = split(",", local.platform.public_subnet_ids)
}

# --- ECR ---

resource "aws_ecr_repository" "uniflow_ecr" {
  name                 = "uniflow/academic-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    projects = var.project-tag
  }
}

# --- Logs ---

resource "aws_cloudwatch_log_group" "uniflow_service" {
  name              = "/ecs/${var.project-tag}-academic-service"
  retention_in_days = var.log_retention_days

  tags = {
    projects = var.project-tag
  }
}

# # --- ECS cluster (Fargate) ---

# resource "aws_ecs_cluster" "uniflow_cluster" {
#   name = "${var.project-tag}-cluster"

#   setting {
#     name  = "containerInsights"
#     value = "disabled"
#   }

#   tags = {
#     projects = var.project-tag
#   }
# }

# --- Security group: the app port is open only to the internal ALB. ---

resource "aws_security_group" "ecs_service" {
  name        = "${var.project-tag}-academic-service-sg"
  description = "Academic service Fargate tasks"
  vpc_id      = local.platform.vpc_id

  tags = {
    projects = var.project-tag
  }
}

resource "aws_vpc_security_group_ingress_rule" "from_alb" {
  security_group_id            = aws_security_group.ecs_service.id
  description                  = "App port from the internal ALB"
  referenced_security_group_id = local.platform.alb_sg_id
  ip_protocol                  = "tcp"
  from_port                    = var.container_port
  to_port                      = var.container_port
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.ecs_service.id
  description       = "ECR, Secrets Manager, EventBridge Scheduler and the database"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# The root RDS security group admits the private-app subnets only; the tasks
# live in the public subnets, so admit this service's security group.
resource "aws_vpc_security_group_ingress_rule" "rds_from_service" {
  security_group_id            = local.platform.rds_sg_id
  description                  = "PostgreSQL from the academic service"
  referenced_security_group_id = aws_security_group.ecs_service.id
  ip_protocol                  = "tcp"
  from_port                    = tonumber(local.platform.rds_port)
  to_port                      = tonumber(local.platform.rds_port)
}

# --- IAM: task execution role (pull de ECR + logs) y task role (permisos de
# la app en runtime: ver las políticas de recordatorios más abajo). ---

data "aws_iam_policy_document" "ecs_task_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_task_execution" {
  name               = "${var.project-tag}-academic-service-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume.json

  tags = {
    projects = var.project-tag
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# The execution role injects the database credentials from the RDS-managed secret.
data "aws_iam_policy_document" "read_db_secret" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [local.platform.rds_secret_arn]
  }
}

resource "aws_iam_role_policy" "ecs_task_execution_db_secret" {
  name   = "read-db-secret"
  role   = aws_iam_role.ecs_task_execution.id
  policy = data.aws_iam_policy_document.read_db_secret.json
}

resource "aws_iam_role" "ecs_task" {
  name               = "${var.project-tag}-academic-service-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume.json

  tags = {
    projects = var.project-tag
  }
}

# Task role: manage reminder schedules in the platform's scheduler group and
# pass the platform's scheduler role (only needed when reminder_scheduler = "eventbridge").
data "aws_iam_policy_document" "ecs_task_reminders" {
  statement {
    actions = [
      "scheduler:CreateSchedule",
      "scheduler:UpdateSchedule",
      "scheduler:DeleteSchedule",
      "scheduler:GetSchedule",
    ]
    resources = ["arn:aws:scheduler:${var.aws_region}:${data.aws_caller_identity.current.account_id}:schedule/${local.platform.scheduler_group_name}/*"]
  }

  statement {
    actions   = ["iam:PassRole"]
    resources = [local.platform.scheduler_role_arn]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["scheduler.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "ecs_task_reminders" {
  count  = var.reminder_scheduler == "eventbridge" ? 1 : 0
  name   = "reminder-schedules"
  role   = aws_iam_role.ecs_task.id
  policy = data.aws_iam_policy_document.ecs_task_reminders.json
}

# --- Task definition ---

resource "aws_ecs_task_definition" "uniflow_service" {
  family                   = "${var.project-tag}-academic-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "academic-service"
      image     = "${aws_ecr_repository.uniflow_ecr.repository_url}:${var.image_tag}"
      essential = true

      environment = [
        { name = "SERVER_PORT", value = tostring(var.container_port) },
        { name = "DB_HOST", value = local.platform.rds_address },
        { name = "DB_PORT", value = local.platform.rds_port },
        { name = "DB_NAME", value = local.platform.rds_db_name },
        { name = "COGNITO_ISSUER_URI", value = local.platform.cognito_issuer_uri },
        { name = "COGNITO_ALLOWED_CLIENT_IDS", value = local.platform.cognito_client_id },
        { name = "CORS_ALLOWED_ORIGINS", value = local.platform.site_url },
        { name = "REMINDER_SCHEDULER", value = var.reminder_scheduler },
        { name = "REMINDER_GROUP_NAME", value = local.platform.scheduler_group_name },
        { name = "REMINDER_QUEUE_ARN", value = local.platform.expirations_queue_arn },
        { name = "REMINDER_ROLE_ARN", value = local.platform.scheduler_role_arn },
      ]

      # RDS-managed secret is a JSON document: {"username": ..., "password": ...}
      secrets = [
        { name = "DB_USER", valueFrom = "${local.platform.rds_secret_arn}:username::" },
        { name = "DB_PASSWORD", valueFrom = "${local.platform.rds_secret_arn}:password::" },
      ]

      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.uniflow_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "academic-service"
        }
      }
    }
  ])

  tags = {
    projects = var.project-tag
  }
}

# --- Service ---

resource "aws_ecs_service" "uniflow_service" {
  name            = "${var.project-tag}-academic-service"
  cluster         = local.platform.ecs_cluster_arn
  task_definition = aws_ecs_task_definition.uniflow_service.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = local.service_subnet_ids
    security_groups  = [aws_security_group.ecs_service.id]
    assign_public_ip = true
  }

  # Spring Boot needs about 60s to start; ignore failing ALB health checks meanwhile.
  health_check_grace_period_seconds = 90

  # Registers the task IP in the internal ALB target group; the HTTP API reaches
  # the ALB through the VPC link.
  load_balancer {
    target_group_arn = local.platform.academic_target_group_arn
    container_name   = "academic-service"
    container_port   = var.container_port
  }

  tags = {
    projects = var.project-tag
  }
}

# --- GitHub Actions OIDC: permite al workflow de CI pushear a ECR sin llaves
# de larga duración. El provider es único por cuenta: esta stack lo crea. Si
# algún día otra stack ya lo tiene, importarlo en lugar de duplicarlo. ---

data "aws_caller_identity" "current" {}

data "tls_certificate" "github" {
  url = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github.certificates[0].sha1_fingerprint]

  tags = {
    projects = var.project-tag
  }
}

data "aws_iam_policy_document" "github_actions_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  name               = "${var.project-tag}-academic-service-github-actions"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume.json

  tags = {
    projects = var.project-tag
  }
}

data "aws_iam_policy_document" "github_actions_ecr_push" {
  statement {
    sid       = "EcrAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
    ]
    resources = [aws_ecr_repository.uniflow_ecr.arn]
  }
}

resource "aws_iam_role_policy" "github_actions_ecr_push" {
  name   = "ecr-push"
  role   = aws_iam_role.github_actions.id
  policy = data.aws_iam_policy_document.github_actions_ecr_push.json
}
