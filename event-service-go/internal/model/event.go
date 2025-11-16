package model

import (
	"time"

	"gorm.io/datatypes"
)

// Event represents an event entity
type Event struct {
	ID         int64          `gorm:"primaryKey;autoIncrement" json:"id"`
	Name       string         `gorm:"type:varchar(255);not null;index" json:"name"`
	UserID     *int64         `gorm:"index" json:"userId,omitempty"`
	Properties datatypes.JSON `gorm:"type:json" json:"properties"`
	CreatedAt  time.Time      `gorm:"autoCreateTime" json:"createdAt"`
	UpdatedAt  time.Time      `gorm:"autoUpdateTime" json:"updatedAt"`
}

// TableName specifies the table name for Event
func (Event) TableName() string {
	return "events"
}
