package repository

import (
	"context"

	"event-service-go/internal/model"

	"gorm.io/gorm"
)

// CampaignEventsRepository handles campaign-event relationship operations
type CampaignEventsRepository struct {
	db *gorm.DB
}

// NewCampaignEventsRepository creates a new campaign events repository
func NewCampaignEventsRepository(db *gorm.DB) *CampaignEventsRepository {
	return &CampaignEventsRepository{db: db}
}

// Create creates a new campaign-event relationship
func (r *CampaignEventsRepository) Create(ctx context.Context, ce *model.CampaignEvents) error {
	return r.db.WithContext(ctx).Create(ce).Error
}
