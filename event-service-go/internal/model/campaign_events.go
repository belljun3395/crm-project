package model

import "time"

// CampaignEvents represents the many-to-many relationship between campaigns and events
type CampaignEvents struct {
	ID         int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	CampaignID int64     `gorm:"not null;index" json:"campaignId"`
	EventID    int64     `gorm:"not null;index" json:"eventId"`
	CreatedAt  time.Time `gorm:"autoCreateTime" json:"createdAt"`

	// Associations
	Campaign *Campaign `gorm:"foreignKey:CampaignID" json:"campaign,omitempty"`
	Event    *Event    `gorm:"foreignKey:EventID" json:"event,omitempty"`
}

// TableName specifies the table name for CampaignEvents
func (CampaignEvents) TableName() string {
	return "campaign_events"
}
