package handler

import (
	"context"

	"event-service-go/internal/dto"
)

// EventServiceInterface defines the interface for EventService
type EventServiceInterface interface {
	CreateEvent(ctx context.Context, req *dto.CreateEventRequest) (*dto.CreateEventResponse, error)
	SearchEvents(ctx context.Context, req *dto.SearchEventsRequest) (*dto.SearchEventsResponse, error)
}

// CampaignServiceInterface defines the interface for CampaignService
type CampaignServiceInterface interface {
	CreateCampaign(ctx context.Context, req *dto.CreateCampaignRequest) (*dto.CreateCampaignResponse, error)
}
