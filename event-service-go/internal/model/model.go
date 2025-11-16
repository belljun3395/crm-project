package model

import (
	"database/sql/driver"
	"encoding/json"
	"errors"
	"time"
)

// Property represents a key-value pair
type Property struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

// Properties is a collection of Property
type Properties []Property

// Value implements driver.Valuer for database storage
func (p Properties) Value() (driver.Value, error) {
	return json.Marshal(p)
}

// Scan implements sql.Scanner for database retrieval
func (p *Properties) Scan(value interface{}) error {
	if value == nil {
		*p = Properties{}
		return nil
	}

	bytes, ok := value.([]byte)
	if !ok {
		return errors.New("failed to unmarshal Properties value")
	}

	return json.Unmarshal(bytes, p)
}

// GetKeys returns all keys from Properties
func (p Properties) GetKeys() []string {
	keys := make([]string, len(p))
	for i, prop := range p {
		keys[i] = prop.Key
	}
	return keys
}

// GetValue returns value for a given key
func (p Properties) GetValue(key string) (string, bool) {
	for _, prop := range p {
		if prop.Key == key {
			return prop.Value, true
		}
	}
	return "", false
}

// Event represents an event entity
type Event struct {
	ID         int64      `json:"id" db:"id"`
	Name       string     `json:"name" db:"name"`
	UserID     int64      `json:"userId" db:"user_id"`
	Properties Properties `json:"properties" db:"properties"`
	CreatedAt  time.Time  `json:"createdAt" db:"created_at"`
}

// Campaign represents a campaign entity
type Campaign struct {
	ID         int64      `json:"id" db:"id"`
	Name       string     `json:"name" db:"name"`
	Properties Properties `json:"properties" db:"properties"`
	CreatedAt  time.Time  `json:"createdAt" db:"created_at"`
}

// AllMatchPropertyKeys checks if campaign properties match given keys
func (c *Campaign) AllMatchPropertyKeys(keys []string) bool {
	campaignKeys := c.Properties.GetKeys()
	if len(campaignKeys) != len(keys) {
		return false
	}

	keyMap := make(map[string]bool)
	for _, key := range campaignKeys {
		keyMap[key] = true
	}

	for _, key := range keys {
		if !keyMap[key] {
			return false
		}
	}
	return true
}

// CampaignEvents represents a campaign-event relationship
type CampaignEvents struct {
	ID         int64     `json:"id" db:"id"`
	CampaignID int64     `json:"campaignId" db:"campaign_id"`
	EventID    int64     `json:"eventId" db:"event_id"`
	CreatedAt  time.Time `json:"createdAt" db:"created_at"`
}

// User represents a user entity (minimal fields needed)
type User struct {
	ID         int64  `json:"id" db:"id"`
	ExternalID string `json:"externalId" db:"external_id"`
}

// Operation types for event search
type Operation string

const (
	OpEqual        Operation = "="
	OpNotEqual     Operation = "!="
	OpGreater      Operation = ">"
	OpGreaterEqual Operation = ">="
	OpLess         Operation = "<"
	OpLessEqual    Operation = "<="
	OpLike         Operation = "like"
	OpBetween      Operation = "between"
)

// JoinOperation types for combining search conditions
type JoinOperation string

const (
	JoinAnd JoinOperation = "and"
	JoinOr  JoinOperation = "or"
	JoinEnd JoinOperation = "end"
)
