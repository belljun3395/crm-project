package dto

import "time"

// CreateCampaignRequest represents the request for creating a campaign
type CreateCampaignRequest struct {
	Name       string        `json:"name" binding:"required" example:"summer-sale"`
	Properties []PropertyDTO `json:"properties" binding:"required,dive"`
}

// CreateCampaignResponse represents the response for creating a campaign
type CreateCampaignResponse struct {
	ID         int64         `json:"id" example:"1"`
	Name       string        `json:"name" example:"summer-sale"`
	Properties []PropertyDTO `json:"properties"`
	CreatedAt  time.Time     `json:"createdAt" example:"2025-11-16T12:00:00Z"`
}

// CampaignDTO represents a campaign data transfer object
type CampaignDTO struct {
	ID         int64         `json:"id" example:"1"`
	Name       string        `json:"name" example:"summer-sale"`
	Properties []PropertyDTO `json:"properties"`
	CreatedAt  time.Time     `json:"createdAt" example:"2025-11-16T12:00:00Z"`
}
