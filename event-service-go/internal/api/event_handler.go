package api

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"

	"event-service-go/internal/model"
	"event-service-go/internal/repository"

	"github.com/gin-gonic/gin"
)

type EventHandler struct {
	eventRepo          *repository.EventRepository
	campaignRepo       *repository.CampaignRepository
	campaignEventsRepo *repository.CampaignEventsRepository
	userRepo           *repository.UserRepository
}

func NewEventHandler(
	eventRepo *repository.EventRepository,
	campaignRepo *repository.CampaignRepository,
	campaignEventsRepo *repository.CampaignEventsRepository,
	userRepo *repository.UserRepository,
) *EventHandler {
	return &EventHandler{
		eventRepo:          eventRepo,
		campaignRepo:       campaignRepo,
		campaignEventsRepo: campaignEventsRepo,
		userRepo:           userRepo,
	}
}

// PostEventRequest represents the request for creating an event
type PostEventRequest struct {
	Name         string            `json:"name" binding:"required"`
	CampaignName string            `json:"campaignName"`
	ExternalID   string            `json:"externalId" binding:"required"`
	Properties   []PropertyRequest `json:"properties" binding:"required"`
}

type PropertyRequest struct {
	Key   string `json:"key" binding:"required"`
	Value string `json:"value" binding:"required"`
}

// PostEventResponse represents the response for creating an event
type PostEventResponse struct {
	Success bool                  `json:"success"`
	Data    PostEventResponseData `json:"data"`
}

type PostEventResponseData struct {
	ID      int64  `json:"id"`
	Message string `json:"message"`
}

// PostEvent handles POST /api/v1/events
func (h *EventHandler) PostEvent(c *gin.Context) {
	var req PostEventRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ctx := context.Background()

	// Find user by external ID
	user, err := h.userRepo.FindByExternalID(ctx, req.ExternalID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to find user"})
		return
	}
	if user == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": fmt.Sprintf("User not found with externalId: %s", req.ExternalID)})
		return
	}

	// Convert properties
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

	if err := h.eventRepo.Create(ctx, event); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create event"})
		return
	}

	message := "Event saved successfully"

	// Handle campaign if provided
	if req.CampaignName != "" {
		campaign, err := h.campaignRepo.FindByName(ctx, req.CampaignName)
		if err != nil {
			log.Printf("Failed to find campaign: %v", err)
			message = "Event saved but not in campaign"
		} else if campaign == nil {
			log.Printf("Campaign not found: %s", req.CampaignName)
			message = "Event saved but not in campaign"
		} else {
			// Check if properties match
			if !campaign.AllMatchPropertyKeys(properties.GetKeys()) {
				log.Printf("Properties mismatch between campaign and event")
				message = "Campaign properties and Event properties mismatch"
			} else {
				// Create campaign event relationship
				ce := &model.CampaignEvents{
					CampaignID: campaign.ID,
					EventID:    event.ID,
					CreatedAt:  time.Now(),
				}
				if err := h.campaignEventsRepo.Create(ctx, ce); err != nil {
					log.Printf("Failed to create campaign event: %v", err)
					message = "Event saved but failed to link with campaign"
				} else {
					message = "Event saved with campaign"
				}
			}
		}
	}

	c.JSON(http.StatusCreated, PostEventResponse{
		Success: true,
		Data: PostEventResponseData{
			ID:      event.ID,
			Message: message,
		},
	})
}

// PostCampaignRequest represents the request for creating a campaign
type PostCampaignRequest struct {
	Name       string            `json:"name" binding:"required"`
	Properties []PropertyRequest `json:"properties" binding:"required"`
}

// PostCampaignResponse represents the response for creating a campaign
type PostCampaignResponse struct {
	Success bool                     `json:"success"`
	Data    PostCampaignResponseData `json:"data"`
}

type PostCampaignResponseData struct {
	ID         int64             `json:"id"`
	Name       string            `json:"name"`
	Properties []PropertyRequest `json:"properties"`
}

// PostCampaign handles POST /api/v1/events/campaign
func (h *EventHandler) PostCampaign(c *gin.Context) {
	var req PostCampaignRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ctx := context.Background()

	// Check if campaign already exists
	exists, err := h.campaignRepo.ExistsByName(ctx, req.Name)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to check campaign existence"})
		return
	}
	if exists {
		c.JSON(http.StatusConflict, gin.H{"error": fmt.Sprintf("Campaign already exists with name: %s", req.Name)})
		return
	}

	// Convert properties
	properties := make(model.Properties, len(req.Properties))
	for i, p := range req.Properties {
		properties[i] = model.Property{Key: p.Key, Value: p.Value}
	}

	// Create campaign
	campaign := &model.Campaign{
		Name:       req.Name,
		Properties: properties,
		CreatedAt:  time.Now(),
	}

	if err := h.campaignRepo.Create(ctx, campaign); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create campaign"})
		return
	}

	c.JSON(http.StatusCreated, PostCampaignResponse{
		Success: true,
		Data: PostCampaignResponseData{
			ID:         campaign.ID,
			Name:       campaign.Name,
			Properties: req.Properties,
		},
	})
}

// SearchEventsResponse represents the response for searching events
type SearchEventsResponse struct {
	Success bool                     `json:"success"`
	Data    SearchEventsResponseData `json:"data"`
}

type SearchEventsResponseData struct {
	Events []EventDTO `json:"events"`
}

type EventDTO struct {
	ID         int64             `json:"id"`
	Name       string            `json:"name"`
	ExternalID *string           `json:"externalId"`
	Properties []PropertyRequest `json:"properties"`
	CreatedAt  time.Time         `json:"createdAt"`
}

// SearchEvents handles GET /api/v1/events
func (h *EventHandler) SearchEvents(c *gin.Context) {
	eventName := c.Query("eventName")
	where := c.Query("where")

	if eventName == "" || where == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "eventName and where parameters are required"})
		return
	}

	ctx := context.Background()

	// Parse where clause
	conditions := strings.Split(where, ",")
	if len(conditions) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid where parameter"})
		return
	}

	var events []model.Event
	var err error

	if len(conditions) == 1 {
		// Single condition
		parts := strings.Split(conditions[0], "&")
		if len(parts) < 4 {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid where format"})
			return
		}

		properties := model.Properties{}
		for i := 0; i < len(parts)-2; i += 2 {
			properties = append(properties, model.Property{
				Key:   parts[i],
				Value: parts[i+1],
			})
		}

		operation := model.Operation(parts[len(parts)-2])
		events, err = h.eventRepo.SearchByProperty(ctx, eventName, properties, operation)
	} else {
		// For simplicity, just search by name for multiple conditions
		// Full implementation would require building complex queries
		events, err = h.eventRepo.FindByName(ctx, eventName)
	}

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to search events"})
		return
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

	users, err := h.userRepo.FindByIDIn(ctx, userIDs)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch users"})
		return
	}

	userMap := make(map[int64]*model.User)
	for i := range users {
		userMap[users[i].ID] = &users[i]
	}

	// Build response
	eventDTOs := make([]EventDTO, len(events))
	for i, event := range events {
		var externalID *string
		if user, ok := userMap[event.UserID]; ok {
			externalID = &user.ExternalID
		}

		properties := make([]PropertyRequest, len(event.Properties))
		for j, prop := range event.Properties {
			properties[j] = PropertyRequest{
				Key:   prop.Key,
				Value: prop.Value,
			}
		}

		eventDTOs[i] = EventDTO{
			ID:         event.ID,
			Name:       event.Name,
			ExternalID: externalID,
			Properties: properties,
			CreatedAt:  event.CreatedAt,
		}
	}

	c.JSON(http.StatusOK, SearchEventsResponse{
		Success: true,
		Data: SearchEventsResponseData{
			Events: eventDTOs,
		},
	})
}
