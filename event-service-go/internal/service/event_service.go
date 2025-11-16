package service

import (
	"context"
	"time"

	"event-service-go/internal/dto"
	"event-service-go/internal/model"
	"event-service-go/internal/repository"
	apperrors "event-service-go/pkg/errors"

	"go.uber.org/zap"
)

// EventService handles event business logic
type EventService struct {
	eventRepo          *repository.EventRepository
	campaignRepo       *repository.CampaignRepository
	campaignEventsRepo *repository.CampaignEventsRepository
	userRepo           *repository.UserRepository
	logger             *zap.Logger
}

// NewEventService creates a new event service
func NewEventService(
	eventRepo *repository.EventRepository,
	campaignRepo *repository.CampaignRepository,
	campaignEventsRepo *repository.CampaignEventsRepository,
	userRepo *repository.UserRepository,
	logger *zap.Logger,
) *EventService {
	return &EventService{
		eventRepo:          eventRepo,
		campaignRepo:       campaignRepo,
		campaignEventsRepo: campaignEventsRepo,
		userRepo:           userRepo,
		logger:             logger,
	}
}

// CreateEvent creates a new event
func (s *EventService) CreateEvent(ctx context.Context, req *dto.CreateEventRequest) (*dto.CreateEventResponse, error) {
	// Find user by external ID
	user, err := s.userRepo.FindByExternalID(ctx, req.ExternalID)
	if err != nil {
		s.logger.Error("Failed to find user", zap.Error(err), zap.String("external_id", req.ExternalID))
		return nil, apperrors.NewDatabaseError("Failed to find user", err)
	}
	if user == nil {
		return nil, apperrors.NewUserNotFoundError(req.ExternalID)
	}

	// Convert DTO properties to model properties
	properties := make(model.Properties, len(req.Properties))
	for i, p := range req.Properties {
		properties[i] = model.Property{Key: p.Key, Value: p.Value}
	}

	// Create event
	event := &model.Event{
		Name:       req.Name,
		UserID:     user.ID,
		Properties: properties,
		CreatedAt:  time.Now(),
	}

	if err := s.eventRepo.Create(ctx, event); err != nil {
		s.logger.Error("Failed to create event", zap.Error(err))
		return nil, apperrors.NewDatabaseError("Failed to create event", err)
	}

	message := "Event saved successfully"

	// Handle campaign if provided
	if req.CampaignName != "" {
		if err := s.linkEventToCampaign(ctx, event, req.CampaignName, properties); err != nil {
			s.logger.Warn("Failed to link event to campaign",
				zap.Error(err),
				zap.String("campaign_name", req.CampaignName),
				zap.Int64("event_id", event.ID))

			// Determine message based on error type
			if appErr, ok := err.(*apperrors.AppError); ok {
				message = appErr.Message
			} else {
				message = "Event saved but failed to link with campaign"
			}
		} else {
			message = "Event saved with campaign"
		}
	}

	return &dto.CreateEventResponse{
		ID:      event.ID,
		Message: message,
	}, nil
}

// linkEventToCampaign links an event to a campaign
func (s *EventService) linkEventToCampaign(ctx context.Context, event *model.Event, campaignName string, properties model.Properties) error {
	campaign, err := s.campaignRepo.FindByName(ctx, campaignName)
	if err != nil {
		return apperrors.NewDatabaseError("Failed to find campaign", err)
	}
	if campaign == nil {
		return apperrors.NewCampaignNotFoundError(campaignName)
	}

	// Check if properties match
	if !campaign.AllMatchPropertyKeys(properties.GetKeys()) {
		return apperrors.NewPropertyMismatchError()
	}

	// Create campaign event relationship
	ce := &model.CampaignEvents{
		CampaignID: campaign.ID,
		EventID:    event.ID,
		CreatedAt:  time.Now(),
	}

	if err := s.campaignEventsRepo.Create(ctx, ce); err != nil {
		return apperrors.NewDatabaseError("Failed to create campaign event", err)
	}

	return nil
}

// SearchEvents searches for events
func (s *EventService) SearchEvents(ctx context.Context, req *dto.SearchEventsRequest) (*dto.SearchEventsResponse, error) {
	// Parse where clause (simplified implementation)
	// For production, implement proper query parser
	var events []model.Event
	var err error

	// For now, just search by name
	events, err = s.eventRepo.FindByName(ctx, req.EventName)
	if err != nil {
		s.logger.Error("Failed to search events", zap.Error(err))
		return nil, apperrors.NewDatabaseError("Failed to search events", err)
	}

	// Get user information
	userIDs := make([]int64, 0, len(events))
	userIDSet := make(map[int64]bool)
	for _, event := range events {
		if !userIDSet[event.UserID] {
			userIDs = append(userIDs, event.UserID)
			userIDSet[event.UserID] = true
		}
	}

	users, err := s.userRepo.FindByIDIn(ctx, userIDs)
	if err != nil {
		s.logger.Error("Failed to fetch users", zap.Error(err))
		return nil, apperrors.NewDatabaseError("Failed to fetch users", err)
	}

	userMap := make(map[int64]*model.User)
	for i := range users {
		userMap[users[i].ID] = &users[i]
	}

	// Build response
	eventDTOs := make([]dto.EventDTO, len(events))
	for i, event := range events {
		var externalID *string
		if user, ok := userMap[event.UserID]; ok {
			externalID = &user.ExternalID
		}

		properties := make([]dto.PropertyDTO, len(event.Properties))
		for j, prop := range event.Properties {
			properties[j] = dto.PropertyDTO{
				Key:   prop.Key,
				Value: prop.Value,
			}
		}

		eventDTOs[i] = dto.EventDTO{
			ID:         event.ID,
			Name:       event.Name,
			ExternalID: externalID,
			Properties: properties,
			CreatedAt:  event.CreatedAt,
		}
	}

	return &dto.SearchEventsResponse{
		Events: eventDTOs,
	}, nil
}
