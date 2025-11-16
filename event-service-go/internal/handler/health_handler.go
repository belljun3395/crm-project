package handler

import (
	"net/http"

	"event-service-go/internal/dto"

	"github.com/gin-gonic/gin"
)

// HealthHandler handles health check requests
type HealthHandler struct{}

// NewHealthHandler creates a new health handler
func NewHealthHandler() *HealthHandler {
	return &HealthHandler{}
}

// HealthCheck godoc
// @Summary      Health check
// @Description  Check if the service is healthy
// @Tags         health
// @Produce      json
// @Success      200 {object} dto.HealthResponse
// @Router       /health [get]
func (h *HealthHandler) HealthCheck(c *gin.Context) {
	c.JSON(http.StatusOK, dto.HealthResponse{
		Status: "healthy",
	})
}
