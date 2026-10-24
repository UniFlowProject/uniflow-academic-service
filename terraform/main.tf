# --- Red: se referencia la VPC/subnets creadas por la stack raíz `terraform/`
# via tags (no remote state, cada stack es un root module independiente).

data "aws_vpc" "selected" {
  tags = {
    Name     = "${var.project-tag}-vpc"
    projects = var.project-tag
  }
}

data "aws_subnets" "private_app" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.selected.id]
  }

  tags = {
    projects = var.project-tag
    Tier     = "private-app"
  }
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

# --- Security group: solo ingreso al container_port desde dentro de la VPC.
# Sin ALB en este alcance; cuando se agregue, apuntar su SG aquí como source. ---

resource "aws_security_group" "ecs_service" {
  name        = "${var.project-tag}-academic-service-sg"
  description = "Academic service Fargate tasks"
  vpc_id      = data.aws_vpc.selected.id

  ingress {
    description = "App port from within the VPC"
    from_port   = var.container_port
    to_port     = var.container_port
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    projects = var.project-tag
  }
}

# --- IAM: task execution role (pull de ECR + logs) y task role (permisos de
# la app en runtime, vacío por ahora — agregar políticas cuando se necesiten). ---

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

resource "aws_iam_role" "ecs_task" {
  name               = "${var.project-tag}-academic-service-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume.json

  tags = {
    projects = var.project-tag
  }
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
  cluster         = aws_ecs_cluster.uniflow_cluster.id
  task_definition = aws_ecs_task_definition.uniflow_service.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.private_app.ids
    security_groups  = [aws_security_group.ecs_service.id]
    assign_public_ip = false
  }

  tags = {
    projects = var.project-tag
  }
}

# --- GitHub Actions OIDC: permite al workflow de CI pushear a ECR sin llaves
# de larga duración. Si la cuenta ya tiene un OIDC provider de GitHub creado
# por otra stack, importarlo (`terraform import`) en vez de dejar que este
# recurso intente crear uno duplicado (AWS rechaza con EntityAlreadyExists). ---

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
