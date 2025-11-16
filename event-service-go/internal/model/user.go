package model

import "time"

// User represents a user entity
type User struct {
	ID         int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	ExternalID string    `gorm:"type:varchar(255);not null;unique;index" json:"externalId"`
	CreatedAt  time.Time `gorm:"autoCreateTime" json:"createdAt"`
	UpdatedAt  time.Time `gorm:"autoUpdateTime" json:"updatedAt"`
}

// TableName specifies the table name for User
func (User) TableName() string {
	return "users"
}
