package repository

import (
	"context"
	"errors"

	"event-service-go/internal/model"

	"gorm.io/gorm"
)

// UserRepository handles user data operations
type UserRepository struct {
	db *gorm.DB
}

// NewUserRepository creates a new user repository
func NewUserRepository(db *gorm.DB) *UserRepository {
	return &UserRepository{db: db}
}

// FindByExternalID finds a user by external ID
func (r *UserRepository) FindByExternalID(ctx context.Context, externalID string) (*model.User, error) {
	var user model.User
	if err := r.db.WithContext(ctx).
		Where("external_id = ?", externalID).
		First(&user).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &user, nil
}

// FindByIDIn finds users by multiple IDs
func (r *UserRepository) FindByIDIn(ctx context.Context, ids []int64) ([]model.User, error) {
	if len(ids) == 0 {
		return []model.User{}, nil
	}

	var users []model.User
	err := r.db.WithContext(ctx).
		Where("id IN ?", ids).
		Find(&users).Error
	return users, err
}
