package repository

import (
	"context"
	"database/sql"
	"fmt"

	"event-service-go/internal/model"
)

type EventRepository struct {
	db *Database
}

func NewEventRepository(db *Database) *EventRepository {
	return &EventRepository{db: db}
}

func (r *EventRepository) Create(ctx context.Context, event *model.Event) error {
	query := `INSERT INTO events (name, user_id, properties, created_at) 
			  VALUES (?, ?, ?, ?)`

	result, err := r.db.ExecContext(ctx, query,
		event.Name, event.UserID, event.Properties, event.CreatedAt)
	if err != nil {
		return err
	}

	id, err := result.LastInsertId()
	if err != nil {
		return err
	}

	event.ID = id
	return nil
}

func (r *EventRepository) FindByName(ctx context.Context, name string) ([]model.Event, error) {
	query := `SELECT id, name, user_id, properties, created_at 
			  FROM events WHERE name = ?`

	rows, err := r.db.QueryContext(ctx, query, name)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []model.Event
	for rows.Next() {
		var event model.Event
		if err := rows.Scan(&event.ID, &event.Name, &event.UserID,
			&event.Properties, &event.CreatedAt); err != nil {
			return nil, err
		}
		events = append(events, event)
	}

	return events, rows.Err()
}

func (r *EventRepository) FindByIDIn(ctx context.Context, ids []int64) ([]model.Event, error) {
	if len(ids) == 0 {
		return []model.Event{}, nil
	}

	query := `SELECT id, name, user_id, properties, created_at 
			  FROM events WHERE id IN (?` + repeat(", ?", len(ids)-1) + `)`

	args := make([]interface{}, len(ids))
	for i, id := range ids {
		args[i] = id
	}

	rows, err := r.db.QueryContext(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []model.Event
	for rows.Next() {
		var event model.Event
		if err := rows.Scan(&event.ID, &event.Name, &event.UserID,
			&event.Properties, &event.CreatedAt); err != nil {
			return nil, err
		}
		events = append(events, event)
	}

	return events, rows.Err()
}

func (r *EventRepository) SearchByProperty(ctx context.Context, eventName string,
	properties model.Properties, operation model.Operation) ([]model.Event, error) {

	query := buildSearchQuery(eventName, []searchCondition{
		{properties: properties, operation: operation, joinOp: model.JoinEnd},
	})

	rows, err := r.db.QueryContext(ctx, query, eventName)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	return scanEvents(rows)
}

type searchCondition struct {
	properties model.Properties
	operation  model.Operation
	joinOp     model.JoinOperation
}

func buildSearchQuery(eventName string, conditions []searchCondition) string {
	query := "SELECT id, name, user_id, properties, created_at FROM events WHERE name = ?"

	for _, cond := range conditions {
		for _, prop := range cond.properties {
			switch cond.operation {
			case model.OpEqual:
				query += fmt.Sprintf(" AND JSON_EXTRACT(properties, '$.%s') = '%s'", prop.Key, prop.Value)
			case model.OpNotEqual:
				query += fmt.Sprintf(" AND JSON_EXTRACT(properties, '$.%s') != '%s'", prop.Key, prop.Value)
			case model.OpGreater:
				query += fmt.Sprintf(" AND CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) > %s", prop.Key, prop.Value)
			case model.OpGreaterEqual:
				query += fmt.Sprintf(" AND CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) >= %s", prop.Key, prop.Value)
			case model.OpLess:
				query += fmt.Sprintf(" AND CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) < %s", prop.Key, prop.Value)
			case model.OpLessEqual:
				query += fmt.Sprintf(" AND CAST(JSON_EXTRACT(properties, '$.%s') AS DECIMAL) <= %s", prop.Key, prop.Value)
			case model.OpLike:
				query += fmt.Sprintf(" AND JSON_EXTRACT(properties, '$.%s') LIKE '%%%s%%'", prop.Key, prop.Value)
			}
		}
	}

	return query
}

func scanEvents(rows *sql.Rows) ([]model.Event, error) {
	var events []model.Event
	for rows.Next() {
		var event model.Event
		if err := rows.Scan(&event.ID, &event.Name, &event.UserID,
			&event.Properties, &event.CreatedAt); err != nil {
			return nil, err
		}
		events = append(events, event)
	}
	return events, rows.Err()
}

func repeat(s string, count int) string {
	result := ""
	for i := 0; i < count; i++ {
		result += s
	}
	return result
}
