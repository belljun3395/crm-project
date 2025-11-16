package repository

import (
	"context"
	"database/sql"
	"errors"
	"event-service-go/internal/model"
	"time"
)

type CampaignRepository struct {
	db    *Database
	cache *RedisCache
}

func NewCampaignRepository(db *Database, cache *RedisCache) *CampaignRepository {
	return &CampaignRepository{db: db, cache: cache}
}

func (r *CampaignRepository) Create(ctx context.Context, campaign *model.Campaign) error {
	query := `INSERT INTO campaigns (name, properties, created_at) 
			  VALUES (?, ?, ?)`

	result, err := r.db.ExecContext(ctx, query,
		campaign.Name, campaign.Properties, campaign.CreatedAt)
	if err != nil {
		return err
	}

	id, err := result.LastInsertId()
	if err != nil {
		return err
	}

	campaign.ID = id

	// Save to cache
	cacheKey := CampaignCacheKey("name", campaign.Name)
	_ = r.cache.Set(ctx, cacheKey, campaign, 24*time.Hour)

	return nil
}

func (r *CampaignRepository) FindByName(ctx context.Context, name string) (*model.Campaign, error) {
	// Try cache first
	cacheKey := CampaignCacheKey("name", name)
	var campaign model.Campaign
	if err := r.cache.Get(ctx, cacheKey, &campaign); err == nil {
		return &campaign, nil
	}

	// Query database
	query := `SELECT id, name, properties, created_at 
			  FROM campaigns WHERE name = ? LIMIT 1`

	err := r.db.QueryRowContext(ctx, query, name).Scan(
		&campaign.ID, &campaign.Name, &campaign.Properties, &campaign.CreatedAt)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}

	// Save to cache
	_ = r.cache.Set(ctx, cacheKey, &campaign, 24*time.Hour)

	return &campaign, nil
}

func (r *CampaignRepository) ExistsByName(ctx context.Context, name string) (bool, error) {
	query := `SELECT EXISTS(SELECT 1 FROM campaigns WHERE name = ?)`

	var exists bool
	err := r.db.QueryRowContext(ctx, query, name).Scan(&exists)
	if err != nil {
		return false, err
	}

	return exists, nil
}
