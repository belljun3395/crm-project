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

// MockCampaignService is a mock for CampaignService
type MockCampaignService struct {
	mock.Mock
}

func (m *MockCampaignService) CreateCampaign(ctx context.Context, req *dto.CreateCampaignRequest) (*dto.CreateCampaignResponse, error) {
	args := m.Called(ctx, req)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*dto.CreateCampaignResponse), args.Error(1)
}

func TestCampaignHandler_CreateCampaign_Success(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockCampaignService)
	logger, _ := zap.NewDevelopment()
	handler := NewCampaignHandler(mockService, logger)

	req := &dto.CreateCampaignRequest{
		Name: "summer-sale",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "template"},
			{Key: "amount", Value: "template"},
		},
	}

	expectedResp := &dto.CreateCampaignResponse{
		ID:   1,
		Name: "summer-sale",
	}

	mockService.On("CreateCampaign", mock.Anything, req).Return(expectedResp, nil)

	// Create request
	body, _ := json.Marshal(req)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("POST", "/api/v2/events/campaign", bytes.NewBuffer(body))
	c.Request.Header.Set("Content-Type", "application/json")

	// Act
	handler.CreateCampaign(c)

	// Assert
	assert.Equal(t, http.StatusCreated, w.Code)
	var response dto.SuccessResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.True(t, response.Success)
	mockService.AssertExpectations(t)
}

func TestCampaignHandler_CreateCampaign_InvalidJSON(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockCampaignService)
	logger, _ := zap.NewDevelopment()
	handler := NewCampaignHandler(mockService, logger)

	// Invalid JSON
	body := []byte(`{invalid json}`)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("POST", "/api/v2/events/campaign", bytes.NewBuffer(body))
	c.Request.Header.Set("Content-Type", "application/json")

	// Act
	handler.CreateCampaign(c)

	// Assert
	assert.Equal(t, http.StatusBadRequest, w.Code)
	var response dto.ErrorResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.False(t, response.Success)
}

func TestCampaignHandler_CreateCampaign_ServiceError(t *testing.T) {
	// Arrange
	gin.SetMode(gin.TestMode)
	mockService := new(MockCampaignService)
	logger, _ := zap.NewDevelopment()
	handler := NewCampaignHandler(mockService, logger)

	req := &dto.CreateCampaignRequest{
		Name: "summer-sale",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "template"},
		},
	}

	mockService.On("CreateCampaign", mock.Anything, req).Return(nil, errors.New("service error"))

	// Create request
	body, _ := json.Marshal(req)
	w := httptest.NewRecorder()
	c, _ := gin.CreateTestContext(w)
	c.Request = httptest.NewRequest("POST", "/api/v2/events/campaign", bytes.NewBuffer(body))
	c.Request.Header.Set("Content-Type", "application/json")

	// Act
	handler.CreateCampaign(c)

	// Assert
	assert.Equal(t, http.StatusInternalServerError, w.Code)
	var response dto.ErrorResponse
	json.Unmarshal(w.Body.Bytes(), &response)
	assert.False(t, response.Success)
	mockService.AssertExpectations(t)
}
