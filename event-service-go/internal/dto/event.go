package dto

import "time"

// PropertyDTO represents a property data transfer object
type PropertyDTO struct {
	Key   string `json:"key" binding:"required" example:"product"`
	Value string `json:"value" binding:"required" example:"laptop"`
}

// CreateEventRequest represents the request for creating an event
type CreateEventRequest struct {
	Name         string        `json:"name" binding:"required" example:"purchase"`
	CampaignName string        `json:"campaignName,omitempty" example:"summer-sale"`
	ExternalID   string        `json:"externalId" binding:"required" example:"user123"`
	Properties   []PropertyDTO `json:"properties" binding:"required,dive"`
}

// CreateEventResponse represents the response for creating an event
type CreateEventResponse struct {
	ID      int64  `json:"id" example:"1"`
	Message string `json:"message" example:"Event saved with campaign"`
}

// EventDTO represents an event data transfer object
type EventDTO struct {
	ID         int64         `json:"id" example:"1"`
	Name       string        `json:"name" example:"purchase"`
	ExternalID *string       `json:"externalId,omitempty" example:"user123"`
	Properties []PropertyDTO `json:"properties"`
	CreatedAt  time.Time     `json:"createdAt" example:"2025-11-16T12:00:00Z"`
}

// SearchEventsRequest represents the request for searching events
type SearchEventsRequest struct {
	EventName string `form:"eventName" binding:"required" example:"purchase"`
	Where     string `form:"where" binding:"required" example:"product&laptop&=&end"`
}

// SearchEventsResponse represents the response for searching events
type SearchEventsResponse struct {
	Events []EventDTO `json:"events"`
}
