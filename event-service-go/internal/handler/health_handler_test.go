package handler

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"event-service-go/internal/dto"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func TestHealthHandler_HealthCheck_Success(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	handler := NewHealthHandler()

	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("GET", "/health", nil)

	// Act
	handler.HealthCheck(c)

	// Assert
	assert.Equal(t, http.StatusOK, w.Code)
	var response dto.HealthResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.Equal(t, "healthy", response.Status)
}
