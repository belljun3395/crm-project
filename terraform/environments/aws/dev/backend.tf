terraform {
  # Remote backend for production use
  # backend "remote" {
  #   hostname     = "app.terraform.io"
  #   organization = "jongjun-org"
  #
  #   workspaces {
  #     name = "jongjun-aws"
  #   }
  # }

  # Local backend for development and validation
  backend "local" {
    path = "terraform.tfstate"
  }
}
