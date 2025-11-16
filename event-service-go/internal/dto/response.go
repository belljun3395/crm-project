package dto

// SuccessResponse represents a successful API response
type SuccessResponse struct {
	Success bool        `json:"success" example:"true"`
	Data    interface{} `json:"data"`
}

// ErrorResponse represents an error API response
type ErrorResponse struct {
	Success bool   `json:"success" example:"false"`
	Error   string `json:"error" example:"User not found"`
	Code    string `json:"code,omitempty" example:"USER_NOT_FOUND"`
}

// HealthResponse represents a health check response
type HealthResponse struct {
	Status string `json:"status" example:"healthy"`
}
