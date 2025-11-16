package service

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
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

	// Marshal properties to JSON
	propertiesJSON, err := json.Marshal(properties)
	if err != nil {
		s.logger.Error("Failed to marshal properties", zap.Error(err))
		return nil, apperrors.NewBadRequestError("Invalid properties format", err)
	}

	// Create event
	event := &model.Event{
		Name:       req.Name,
		UserID:     &user.ID,
		Properties: propertiesJSON,
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
	var events []model.Event
	var err error

	// Parse where clause and search by properties
	if req.Where != "" {
		properties, operation, err := parseWhereClause(req.Where)
		if err != nil {
			return nil, apperrors.NewBadRequestError("Invalid where clause", err)
		}

		events, err = s.eventRepo.SearchByProperty(ctx, req.EventName, properties, operation)
		if err != nil {
			s.logger.Error("Failed to search events by property", zap.Error(err))
			return nil, apperrors.NewDatabaseError("Failed to search events", err)
		}
	} else {
		// If no where clause, just search by name
		events, err = s.eventRepo.FindByName(ctx, req.EventName)
		if err != nil {
			s.logger.Error("Failed to search events", zap.Error(err))
			return nil, apperrors.NewDatabaseError("Failed to search events", err)
		}
	}

	// Get user information
	userIDs := make([]int64, 0, len(events))
	userIDSet := make(map[int64]bool)
	for _, event := range events {
		if event.UserID != nil && !userIDSet[*event.UserID] {
			userIDs = append(userIDs, *event.UserID)
			userIDSet[*event.UserID] = true
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
		if event.UserID != nil {
			if user, ok := userMap[*event.UserID]; ok {
				externalID = &user.ExternalID
			}
		}

		// Unmarshal properties from JSON
		var props model.Properties
		if err := json.Unmarshal(event.Properties, &props); err != nil {
			s.logger.Warn("Failed to unmarshal event properties",
				zap.Error(err),
				zap.Int64("event_id", event.ID))
			props = model.Properties{}
		}

		properties := make([]dto.PropertyDTO, len(props))
		for j, prop := range props {
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

// parseWhereClause parses the where clause string into properties and operation
// Format: key&value&operation&joinOperation
// Example: product&laptop&=&end
func parseWhereClause(where string) (model.Properties, model.Operation, error) {
	parts := strings.Split(where, "&")
	if len(parts) < 4 {
		return nil, "", fmt.Errorf("where clause must have at least 4 parts (key&value&operation&joinOperation)")
	}

	// Parse each property query (grouped by 4)
	var properties model.Properties
	var operation model.Operation

	for i := 0; i+3 < len(parts); i += 4 {
		key := parts[i]
		value := parts[i+1]
		op := parts[i+2]
		join := parts[i+3]

		// Validate property key
		if err := model.ValidatePropertyKey(key); err != nil {
			return nil, "", fmt.Errorf("invalid property key '%s': %w", key, err)
		}

		// Parse operation
		parsedOp, err := parseOperation(op)
		if err != nil {
			return nil, "", err
		}

		// For now, we use the same operation for all properties
		// In a more complex implementation, we'd support different operations per property
		if operation == "" {
			operation = parsedOp
		} else if operation != parsedOp {
			return nil, "", fmt.Errorf("mixed operations not supported in current implementation")
		}

		properties = append(properties, model.Property{
			Key:   key,
			Value: value,
		})

		// If join operation is "end", we're done
		if join == "end" {
			break
		}
		// "and" and "or" join operations would be handled in a more complex implementation
	}

	if len(properties) == 0 {
		return nil, "", fmt.Errorf("no properties found in where clause")
	}

	return properties, operation, nil
}

// parseOperation converts string operation to model.Operation
func parseOperation(op string) (model.Operation, error) {
	switch op {
	case "=":
		return model.OpEqual, nil
	case "!=":
		return model.OpNotEqual, nil
	case ">":
		return model.OpGreater, nil
	case ">=":
		return model.OpGreaterEqual, nil
	case "<":
		return model.OpLess, nil
	case "<=":
		return model.OpLessEqual, nil
	case "like":
		return model.OpLike, nil
	default:
		return "", fmt.Errorf("unsupported operation: %s", op)
	}
}
