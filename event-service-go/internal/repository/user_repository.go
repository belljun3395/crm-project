package repository

import (
	"context"
	"database/sql"
	"errors"
	"event-service-go/internal/model"
)

type UserRepository struct {
	db *Database
}

func NewUserRepository(db *Database) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) FindByExternalID(ctx context.Context, externalID string) (*model.User, error) {
	query := `SELECT id, external_id FROM users WHERE external_id = ? LIMIT 1`

	var user model.User
	err := r.db.QueryRowContext(ctx, query, externalID).Scan(&user.ID, &user.ExternalID)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}

	return &user, nil
}

func (r *UserRepository) FindByIDIn(ctx context.Context, ids []int64) ([]model.User, error) {
	if len(ids) == 0 {
		return []model.User{}, nil
	}

	query := `SELECT id, external_id FROM users WHERE id IN (?` + repeat(", ?", len(ids)-1) + `)`

	args := make([]interface{}, len(ids))
	for i, id := range ids {
		args[i] = id
	}

	rows, err := r.db.QueryContext(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var users []model.User
	for rows.Next() {
		var user model.User
		if err := rows.Scan(&user.ID, &user.ExternalID); err != nil {
			return nil, err
		}
		users = append(users, user)
	}

	return users, rows.Err()
}
