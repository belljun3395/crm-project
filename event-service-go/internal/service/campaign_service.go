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

// CampaignService handles campaign business logic
type CampaignService struct {
	campaignRepo *repository.CampaignRepository
	logger       *zap.Logger
}

// NewCampaignService creates a new campaign service
func NewCampaignService(
	campaignRepo *repository.CampaignRepository,
	logger *zap.Logger,
) *CampaignService {
	return &CampaignService{
		campaignRepo: campaignRepo,
		logger:       logger,
	}
}

// CreateCampaign creates a new campaign
func (s *CampaignService) CreateCampaign(ctx context.Context, req *dto.CreateCampaignRequest) (*dto.CreateCampaignResponse, error) {
	// Check if campaign already exists
	exists, err := s.campaignRepo.ExistsByName(ctx, req.Name)
	if err != nil {
		s.logger.Error("Failed to check campaign existence", zap.Error(err))
		return nil, apperrors.NewDatabaseError("Failed to check campaign existence", err)
	}
	if exists {
		return nil, apperrors.NewCampaignExistsError(req.Name)
	}

	// Convert DTO properties to model properties
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

	if err := s.campaignRepo.Create(ctx, campaign); err != nil {
		s.logger.Error("Failed to create campaign", zap.Error(err))
		return nil, apperrors.NewDatabaseError("Failed to create campaign", err)
	}

	return &dto.CreateCampaignResponse{
		ID:         campaign.ID,
		Name:       campaign.Name,
		Properties: req.Properties,
		CreatedAt:  campaign.CreatedAt,
	}, nil
}
