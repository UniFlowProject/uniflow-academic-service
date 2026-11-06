terraform {
  required_version = "1.15.9"

  # Bucket/region come from backend.hcl (gitignored): terraform init -backend-config=backend.hcl
  backend "s3" {
    key          = "academic/terraform.tfstate"
    use_lockfile = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}
