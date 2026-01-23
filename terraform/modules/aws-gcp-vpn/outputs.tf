output "aws_vpn_gateway_id" {
  description = "AWS VPN Gateway ID"
  value       = aws_vpn_gateway.main.id
}

output "gcp_vpn_gateway_id" {
  description = "GCP HA VPN Gateway ID"
  value       = google_compute_ha_vpn_gateway.main.id
}

output "tunnel_1_status" {
  description = "Status of VPN tunnel 1"
  value = {
    aws_tunnel_address = aws_vpn_connection.tunnel_1.tunnel1_address
    gcp_tunnel_name    = google_compute_vpn_tunnel.tunnel_1.name
  }
}

output "tunnel_2_status" {
  description = "Status of VPN tunnel 2"
  value = {
    aws_tunnel_address = aws_vpn_connection.tunnel_2.tunnel1_address
    gcp_tunnel_name    = google_compute_vpn_tunnel.tunnel_2.name
  }
}

output "gcp_router_name" {
  description = "GCP Cloud Router name"
  value       = google_compute_router.router.name
}
