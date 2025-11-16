package handler

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"event-service-go/internal/dto"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"go.uber.org/zap"
)

// MockEventService is a mock for EventService
type MockEventService struct {
	mock.Mock
}

func (m *MockEventService) CreateEvent(ctx context.Context, req *dto.CreateEventRequest) (*dto.CreateEventResponse, error) {
	args := m.Called(ctx, req)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*dto.CreateEventResponse), args.Error(1)
}

func (m *MockEventService) SearchEvents(ctx context.Context, req *dto.SearchEventsRequest) (*dto.SearchEventsResponse, error) {
	args := m.Called(ctx, req)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*dto.SearchEventsResponse), args.Error(1)
}

func TestEventHandler_CreateEvent_Success(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockEventService)
	logger, _ := zap.NewDevelopment()
	handler := NewEventHandler(mockService, logger)

	req := &dto.CreateEventRequest{
		Name:       "purchase",
		ExternalID: "user123",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "laptop"},
		},
	}

	expectedResp := &dto.CreateEventResponse{
		ID:      1,
		Message: "Event saved successfully",
	}

	mockService.On("CreateEvent", mock.Anything, req).Return(expectedResp, nil)

	// Create request
	body, _ := json.Marshal(req)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("POST", "/api/v2/events", bytes.NewBuffer(body))
	c.Request.Header.Set("Content-Type", "application/json")

	// Act
	handler.CreateEvent(c)

	// Assert
	assert.Equal(t, http.StatusCreated, w.Code)
	var response dto.SuccessResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.True(t, response.Success)
	mockService.AssertExpectations(t)
}

func TestEventHandler_CreateEvent_InvalidJSON(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockEventService)
	logger, _ := zap.NewDevelopment()
	handler := NewEventHandler(mockService, logger)

	// Invalid JSON
	body := []byte(`{invalid json}`)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("POST", "/api/v2/events", bytes.NewBuffer(body))
	c.Request.Header.Set("Content-Type", "application/json")

	// Act
	handler.CreateEvent(c)

	// Assert
	assert.Equal(t, http.StatusBadRequest, w.Code)
	var response dto.ErrorResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.False(t, response.Success)
}

func TestEventHandler_CreateEvent_ServiceError(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockEventService)
	logger, _ := zap.NewDevelopment()
	handler := NewEventHandler(mockService, logger)

	req := &dto.CreateEventRequest{
		Name:       "purchase",
		ExternalID: "user123",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "laptop"},
		},
	}

	mockService.On("CreateEvent", mock.Anything, req).Return(nil, errors.New("service error"))

	// Create request
	body, _ := json.Marshal(req)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("POST", "/api/v2/events", bytes.NewBuffer(body))
	c.Request.Header.Set("Content-Type", "application/json")

	// Act
	handler.CreateEvent(c)

	// Assert
	assert.Equal(t, http.StatusInternalServerError, w.Code)
	var response dto.ErrorResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.False(t, response.Success)
	mockService.AssertExpectations(t)
}

func TestEventHandler_SearchEvents_Success(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockEventService)
	logger, _ := zap.NewDevelopment()
	handler := NewEventHandler(mockService, logger)

	expectedResp := &dto.SearchEventsResponse{
		Events: []dto.EventDTO{
			{
				ID:   1,
				Name: "purchase",
			},
		},
	}

	mockService.On("SearchEvents", mock.Anything, mock.AnythingOfType("*dto.SearchEventsRequest")).
		Return(expectedResp, nil)

	// Create request
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("GET", "/api/v2/events?eventName=purchase&where=product&laptop&=&end", nil)

	// Act
	handler.SearchEvents(c)

	// Assert
	assert.Equal(t, http.StatusOK, w.Code)
	var response dto.SuccessResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.True(t, response.Success)
	mockService.AssertExpectations(t)
}
