package repository

import (
	"context"
	"errors"
	"time"

	"event-service-go/internal/model"

	"gorm.io/gorm"
)

// CampaignRepository handles campaign data operations
type CampaignRepository struct {
	db    *gorm.DB
	cache *RedisCache
}

// NewCampaignRepository creates a new campaign repository
func NewCampaignRepository(db *gorm.DB, cache *RedisCache) *CampaignRepository {
	return &CampaignRepository{db: db, cache: cache}
}

// Create creates a new campaign
func (r *CampaignRepository) Create(ctx context.Context, campaign *model.Campaign) error {
	if err := r.db.WithContext(ctx).Create(campaign).Error; err != nil {
		return err
	}

	// Save to cache
	cacheKey := CampaignCacheKey("name", campaign.Name)
	_ = r.cache.Set(ctx, cacheKey, campaign, 24*time.Hour)

	return nil
}

// FindByName finds a campaign by name with cache
func (r *CampaignRepository) FindByName(ctx context.Context, name string) (*model.Campaign, error) {
	// Try cache first
	cacheKey := CampaignCacheKey("name", name)
	var campaign model.Campaign
	if err := r.cache.Get(ctx, cacheKey, &campaign); err == nil {
		return &campaign, nil
	}

	// Query database
	if err := r.db.WithContext(ctx).
		Where("name = ?", name).
		First(&campaign).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}

	// Save to cache
	_ = r.cache.Set(ctx, cacheKey, &campaign, 24*time.Hour)

	return &campaign, nil
}

// ExistsByName checks if a campaign exists by name
func (r *CampaignRepository) ExistsByName(ctx context.Context, name string) (bool, error) {
	var count int64
	err := r.db.WithContext(ctx).
		Model(&model.Campaign{}).
		Where("name = ?", name).
		Count(&count).Error
	return count > 0, err
}
