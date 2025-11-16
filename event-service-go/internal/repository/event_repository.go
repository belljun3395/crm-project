package repository

import (
	"context"
	"fmt"

	"event-service-go/internal/model"

	"gorm.io/gorm"
)

// EventRepository handles event data operations
type EventRepository struct {
	db *gorm.DB
}

// NewEventRepository creates a new event repository
func NewEventRepository(db *gorm.DB) *EventRepository {
	return &EventRepository{db: db}
}

// Create creates a new event
func (r *EventRepository) Create(ctx context.Context, event *model.Event) error {
	return r.db.WithContext(ctx).Create(event).Error
}

// FindByName finds events by name
func (r *EventRepository) FindByName(ctx context.Context, name string) ([]model.Event, error) {
	var events []model.Event
	err := r.db.WithContext(ctx).
		Where("name = ?", name).
		Find(&events).Error
	return events, err
}

// FindByIDIn finds events by multiple IDs
func (r *EventRepository) FindByIDIn(ctx context.Context, ids []int64) ([]model.Event, error) {
	if len(ids) == 0 {
		return []model.Event{}, nil
	}

	var events []model.Event
	err := r.db.WithContext(ctx).
		Where("id IN ?", ids).
		Find(&events).Error
	return events, err
}

// SearchByProperty searches events by property with operation
func (r *EventRepository) SearchByProperty(ctx context.Context, eventName string,
	properties model.Properties, operation model.Operation) ([]model.Event, error) {

	var events []model.Event
	query := r.db.WithContext(ctx).Where("name = ?", eventName)

	// Build JSON search conditions
	for _, prop := range properties {
		switch operation {
		case model.OpEqual:
			query = query.Where(fmt.Sprintf("JSON_EXTRACT(properties, '$.%s') = ?", prop.Key), prop.Value)
		case model.OpNotEqual:
			query = query.Where(fmt.Sprintf("JSON_EXTRACT(properties, '$.%s') != ?", prop.Key), prop.Value)
		case model.OpGreater:
			query = query.Where(fmt.Sprintf("CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) > ?", prop.Key), prop.Value)
		case model.OpGreaterEqual:
			query = query.Where(fmt.Sprintf("CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) >= ?", prop.Key), prop.Value)
		case model.OpLess:
			query = query.Where(fmt.Sprintf("CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) < ?", prop.Key), prop.Value)
		case model.OpLessEqual:
			query = query.Where(fmt.Sprintf("CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) <= ?", prop.Key), prop.Value)
		case model.OpLike:
			query = query.Where(fmt.Sprintf("JSON_EXTRACT(properties, '$.%s') LIKE ?", prop.Key), "%"+prop.Value+"%")
		}
	}

	err := query.Find(&events).Error
	return events, err
}
