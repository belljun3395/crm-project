package repository

import (
	"context"
	"event-service-go/internal/model"
)

type CampaignEventsRepository struct {
	db *Database
}

func NewCampaignEventsRepository(db *Database) *CampaignEventsRepository {
	return &CampaignEventsRepository{db: db}
}

func (r *CampaignEventsRepository) Create(ctx context.Context, ce *model.CampaignEvents) error {
	query := `INSERT INTO campaign_events (campaign_id, event_id, created_at) 
			  VALUES (?, ?, ?)`

	result, err := r.db.ExecContext(ctx, query,
		ce.CampaignID, ce.EventID, ce.CreatedAt)
	if err != nil {
		return err
	}

	id, err := result.LastInsertId()
	if err != nil {
		return err
	}

	ce.ID = id
	return nil
}
