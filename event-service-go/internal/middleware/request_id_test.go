package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
)

func TestRequestID_GeneratesNewID(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.Use(RequestID())
	router.GET("/test", func(c *gin.Context) {
		requestID := GetRequestID(c)
		c.String(http.StatusOK, requestID)
	})

	// Act
	w := httptest.NewRecorder()
	req := httptest.NewRequest("GET", "/test", nil)
	router.ServeHTTP(w, req)

	// Assert
	assert.Equal(t, http.StatusOK, w.Code)
	requestID := w.Body.String()
	assert.NotEmpty(t, requestID)
	assert.Equal(t, requestID, w.Header().Get(RequestIDKey))
}

func TestRequestID_UsesExistingID(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	router := gin.New()
	router.Use(RequestID())
	router.GET("/test", func(c *gin.Context) {
		requestID := GetRequestID(c)
		c.String(http.StatusOK, requestID)
	})

	existingID := "existing-request-id"

	// Act
	w := httptest.NewRecorder()
	req := httptest.NewRequest("GET", "/test", nil)
	req.Header.Set(RequestIDKey, existingID)
	router.ServeHTTP(w, req)

	// Assert
	assert.Equal(t, http.StatusOK, w.Code)
	requestID := w.Body.String()
	assert.Equal(t, existingID, requestID)
	assert.Equal(t, existingID, w.Header().Get(RequestIDKey))
}

func TestGetRequestID_ReturnsEmpty_WhenNotSet(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	c, _ := gin.CreateTestContext(httptest.NewRecorder())

	// Act
	requestID := GetRequestID(c)

	// Assert
	assert.Empty(t, requestID)
}

func TestGetRequestID_SafeTypeAssertion(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	c, _ := gin.CreateTestContext(httptest.NewRecorder())
	// Set non-string value
	c.Set(RequestIDKey, 12345)

	// Act
	requestID := GetRequestID(c)

	// Assert - should return empty string, not panic
	assert.Empty(t, requestID)
}
