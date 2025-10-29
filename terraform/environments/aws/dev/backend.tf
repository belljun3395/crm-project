terraform {
  backend "remote" {
    hostname     = "app.terraform.io"
    organization = "REPLACE_WITH_YOUR_ORG"

    workspaces {
      name = "crm-dev"
    }
  }
}
