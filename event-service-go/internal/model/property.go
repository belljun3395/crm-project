package model

import (
	"fmt"
	"regexp"
)

// Property represents a key-value property
type Property struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

// propertyKeyRegex validates that property keys only contain alphanumeric characters and underscores
var propertyKeyRegex = regexp.MustCompile(`^[a-zA-Z0-9_]+$`)

// ValidatePropertyKey validates that a property key is safe to use in SQL queries
func ValidatePropertyKey(key string) error {
	if key == "" {
		return fmt.Errorf("property key cannot be empty")
	}
	if len(key) > 255 {
		return fmt.Errorf("property key too long (max 255 characters)")
	}
	if !propertyKeyRegex.MatchString(key) {
		return fmt.Errorf("property key must contain only alphanumeric characters and underscores")
	}
	return nil
}

// Properties is a slice of Property with helper methods
type Properties []Property

// GetKeys returns all property keys
func (p Properties) GetKeys() []string {
	keys := make([]string, len(p))
	for i, prop := range p {
		keys[i] = prop.Key
	}
	return keys
}

// AllMatchPropertyKeys checks if all properties have matching keys
func (p Properties) AllMatchPropertyKeys(keys []string) bool {
	if len(p) != len(keys) {
		return false
	}

	keyMap := make(map[string]bool)
	for _, key := range keys {
		keyMap[key] = true
	}

	for _, prop := range p {
		if !keyMap[prop.Key] {
			return false
		}
	}

	return true
}

// Operation represents a comparison operation for property searches
type Operation string

const (
	OpEqual        Operation = "="
	OpNotEqual     Operation = "!="
	OpGreater      Operation = ">"
	OpGreaterEqual Operation = ">="
	OpLess         Operation = "<"
	OpLessEqual    Operation = "<="
	OpLike         Operation = "like"
)
