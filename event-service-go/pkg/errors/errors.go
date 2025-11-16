package errors

import (
	"fmt"
	"net/http"
)

// AppError represents an application error with HTTP status code
type AppError struct {
	Code       string `json:"code"`
	Message    string `json:"message"`
	StatusCode int    `json:"-"`
	Err        error  `json:"-"`
}

// Error implements error interface
func (e *AppError) Error() string {
	if e.Err != nil {
		return fmt.Sprintf("%s: %v", e.Message, e.Err)
	}
	return e.Message
}

// Unwrap returns the wrapped error
func (e *AppError) Unwrap() error {
	return e.Err
}

// Error codes
const (
	ErrCodeBadRequest          = "BAD_REQUEST"
	ErrCodeNotFound            = "NOT_FOUND"
	ErrCodeConflict            = "CONFLICT"
	ErrCodeInternalServer      = "INTERNAL_SERVER_ERROR"
	ErrCodeValidation          = "VALIDATION_ERROR"
	ErrCodeUserNotFound        = "USER_NOT_FOUND"
	ErrCodeCampaignNotFound    = "CAMPAIGN_NOT_FOUND"
	ErrCodeCampaignExists      = "CAMPAIGN_ALREADY_EXISTS"
	ErrCodePropertyMismatch    = "PROPERTY_MISMATCH"
	ErrCodeDatabaseError       = "DATABASE_ERROR"
	ErrCodeCacheError          = "CACHE_ERROR"
)

// NewBadRequestError creates a bad request error
func NewBadRequestError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeBadRequest,
		Message:    message,
		StatusCode: http.StatusBadRequest,
		Err:        err,
	}
}

// NewNotFoundError creates a not found error
func NewNotFoundError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeNotFound,
		Message:    message,
		StatusCode: http.StatusNotFound,
		Err:        err,
	}
}

// NewConflictError creates a conflict error
func NewConflictError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeConflict,
		Message:    message,
		StatusCode: http.StatusConflict,
		Err:        err,
	}
}

// NewInternalServerError creates an internal server error
func NewInternalServerError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeInternalServer,
		Message:    message,
		StatusCode: http.StatusInternalServerError,
		Err:        err,
	}
}

// NewValidationError creates a validation error
func NewValidationError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeValidation,
		Message:    message,
		StatusCode: http.StatusBadRequest,
		Err:        err,
	}
}

// NewUserNotFoundError creates a user not found error
func NewUserNotFoundError(externalID string) *AppError {
	return &AppError{
		Code:       ErrCodeUserNotFound,
		Message:    fmt.Sprintf("User not found with externalId: %s", externalID),
		StatusCode: http.StatusNotFound,
	}
}

// NewCampaignNotFoundError creates a campaign not found error
func NewCampaignNotFoundError(name string) *AppError {
	return &AppError{
		Code:       ErrCodeCampaignNotFound,
		Message:    fmt.Sprintf("Campaign not found with name: %s", name),
		StatusCode: http.StatusNotFound,
	}
}

// NewCampaignExistsError creates a campaign already exists error
func NewCampaignExistsError(name string) *AppError {
	return &AppError{
		Code:       ErrCodeCampaignExists,
		Message:    fmt.Sprintf("Campaign already exists with name: %s", name),
		StatusCode: http.StatusConflict,
	}
}

// NewPropertyMismatchError creates a property mismatch error
func NewPropertyMismatchError() *AppError {
	return &AppError{
		Code:       ErrCodePropertyMismatch,
		Message:    "Campaign properties and Event properties mismatch",
		StatusCode: http.StatusBadRequest,
	}
}

// NewDatabaseError creates a database error
func NewDatabaseError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeDatabaseError,
		Message:    message,
		StatusCode: http.StatusInternalServerError,
		Err:        err,
	}
}

// NewCacheError creates a cache error
func NewCacheError(message string, err error) *AppError {
	return &AppError{
		Code:       ErrCodeCacheError,
		Message:    message,
		StatusCode: http.StatusInternalServerError,
		Err:        err,
	}
}
