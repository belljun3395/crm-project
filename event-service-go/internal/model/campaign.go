package model

import (
	"encoding/json"
	"time"

	"gorm.io/datatypes"
)

// Campaign represents a campaign entity
type Campaign struct {
	ID         int64          `gorm:"primaryKey;autoIncrement" json:"id"`
	Name       string         `gorm:"type:varchar(255);not null;unique;index" json:"name"`
	Properties datatypes.JSON `gorm:"type:json" json:"properties"`
	CreatedAt  time.Time      `gorm:"autoCreateTime" json:"createdAt"`
	UpdatedAt  time.Time      `gorm:"autoUpdateTime" json:"updatedAt"`
}

// TableName specifies the table name for Campaign
func (Campaign) TableName() string {
	return "campaigns"
}

// AllMatchPropertyKeys checks if all given keys match the campaign's property keys
func (c *Campaign) AllMatchPropertyKeys(keys []string) bool {
	if c.Properties == nil {
		return len(keys) == 0
	}

	// Parse campaign properties
	var props Properties
	if err := json.Unmarshal(c.Properties, &props); err != nil {
		return false
	}

	return props.AllMatchPropertyKeys(keys)
}
