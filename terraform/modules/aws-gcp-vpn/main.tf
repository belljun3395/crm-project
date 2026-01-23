terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
  }
}

# AWS VPN Gateway
resource "aws_vpn_gateway" "main" {
  vpc_id = var.aws_vpc_id

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-vpn-gateway"
    }
  )
}

resource "aws_vpn_gateway_attachment" "main" {
  vpc_id         = var.aws_vpc_id
  vpn_gateway_id = aws_vpn_gateway.main.id
}

# GCP HA VPN Gateway
resource "google_compute_ha_vpn_gateway" "main" {
  name    = "${var.name_prefix}-ha-vpn-gateway"
  network = var.gcp_network_id
  region  = var.gcp_region
}

# Cloud Router for BGP
resource "google_compute_router" "router" {
  name    = "${var.name_prefix}-router"
  network = var.gcp_network_id
  region  = var.gcp_region

  bgp {
    asn               = var.gcp_bgp_asn
    advertise_mode    = "CUSTOM"
    advertised_groups = ["ALL_SUBNETS"]

    # AWS VPC CIDR 광고
    dynamic "advertised_ip_ranges" {
      for_each = var.advertised_ip_ranges
      content {
        range = advertised_ip_ranges.value
      }
    }
  }
}

# AWS Customer Gateway (GCP쪽)
resource "aws_customer_gateway" "gcp_interface_0" {
  bgp_asn    = var.gcp_bgp_asn
  ip_address = google_compute_ha_vpn_gateway.main.vpn_interfaces[0].ip_address
  type       = "ipsec.1"

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-gcp-cgw-0"
    }
  )
}

resource "aws_customer_gateway" "gcp_interface_1" {
  bgp_asn    = var.gcp_bgp_asn
  ip_address = google_compute_ha_vpn_gateway.main.vpn_interfaces[1].ip_address
  type       = "ipsec.1"

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-gcp-cgw-1"
    }
  )
}

# AWS VPN Connections
resource "aws_vpn_connection" "tunnel_1" {
  vpn_gateway_id      = aws_vpn_gateway.main.id
  customer_gateway_id = aws_customer_gateway.gcp_interface_0.id
  type                = "ipsec.1"
  static_routes_only  = false

  tunnel1_inside_cidr   = var.tunnel1_inside_cidr
  tunnel1_preshared_key = var.tunnel1_preshared_key

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-vpn-tunnel-1"
    }
  )
}

resource "aws_vpn_connection" "tunnel_2" {
  vpn_gateway_id      = aws_vpn_gateway.main.id
  customer_gateway_id = aws_customer_gateway.gcp_interface_1.id
  type                = "ipsec.1"
  static_routes_only  = false

  tunnel1_inside_cidr   = var.tunnel2_inside_cidr
  tunnel1_preshared_key = var.tunnel2_preshared_key

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-vpn-tunnel-2"
    }
  )
}

# GCP External VPN Gateway (AWS쪽)
resource "google_compute_external_vpn_gateway" "aws" {
  name            = "${var.name_prefix}-aws-peer-gateway"
  redundancy_type = "TWO_IPS_REDUNDANCY"

  interface {
    id         = 0
    ip_address = aws_vpn_connection.tunnel_1.tunnel1_address
  }

  interface {
    id         = 1
    ip_address = aws_vpn_connection.tunnel_2.tunnel1_address
  }
}

# GCP VPN Tunnels
resource "google_compute_vpn_tunnel" "tunnel_1" {
  name                            = "${var.name_prefix}-tunnel-1"
  region                          = var.gcp_region
  vpn_gateway                     = google_compute_ha_vpn_gateway.main.id
  peer_external_gateway           = google_compute_external_vpn_gateway.aws.id
  peer_external_gateway_interface = 0
  shared_secret                   = var.tunnel1_preshared_key
  router                          = google_compute_router.router.id
  vpn_gateway_interface           = 0
  ike_version                     = 2
}

resource "google_compute_vpn_tunnel" "tunnel_2" {
  name                            = "${var.name_prefix}-tunnel-2"
  region                          = var.gcp_region
  vpn_gateway                     = google_compute_ha_vpn_gateway.main.id
  peer_external_gateway           = google_compute_external_vpn_gateway.aws.id
  peer_external_gateway_interface = 1
  shared_secret                   = var.tunnel2_preshared_key
  router                          = google_compute_router.router.id
  vpn_gateway_interface           = 1
  ike_version                     = 2
}

# GCP Router Interfaces
resource "google_compute_router_interface" "interface_1" {
  name       = "${var.name_prefix}-interface-1"
  router     = google_compute_router.router.name
  region     = var.gcp_region
  ip_range   = var.tunnel1_inside_cidr
  vpn_tunnel = google_compute_vpn_tunnel.tunnel_1.name
}

resource "google_compute_router_interface" "interface_2" {
  name       = "${var.name_prefix}-interface-2"
  router     = google_compute_router.router.name
  region     = var.gcp_region
  ip_range   = var.tunnel2_inside_cidr
  vpn_tunnel = google_compute_vpn_tunnel.tunnel_2.name
}

# BGP Peers
resource "google_compute_router_peer" "peer_1" {
  name                      = "${var.name_prefix}-peer-1"
  router                    = google_compute_router.router.name
  region                    = var.gcp_region
  peer_ip_address           = cidrhost(var.tunnel1_inside_cidr, 1)
  peer_asn                  = var.aws_bgp_asn
  advertised_route_priority = 100
  interface                 = google_compute_router_interface.interface_1.name
}

resource "google_compute_router_peer" "peer_2" {
  name                      = "${var.name_prefix}-peer-2"
  router                    = google_compute_router.router.name
  region                    = var.gcp_region
  peer_ip_address           = cidrhost(var.tunnel2_inside_cidr, 1)
  peer_asn                  = var.aws_bgp_asn
  advertised_route_priority = 100
  interface                 = google_compute_router_interface.interface_2.name
}


# Allow traffic from AWS VPC
resource "google_compute_firewall" "allow_aws" {
  name    = "${var.name_prefix}-allow-aws-ingress"
  network = var.gcp_network_id

  allow {
    protocol = "all"
  }

  source_ranges = var.remote_cidr_ranges
  priority      = 1000
}
