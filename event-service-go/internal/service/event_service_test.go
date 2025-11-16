package service

import (
	"context"
	"errors"
	"testing"
	"time"

	"event-service-go/internal/dto"
	"event-service-go/internal/model"
	apperrors "event-service-go/pkg/errors"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"go.uber.org/zap"
)

// MockEventRepository is a mock for EventRepository
type MockEventRepository struct {
	mock.Mock
}

func (m *MockEventRepository) Create(ctx context.Context, event *model.Event) error {
	args := m.Called(ctx, event)
	if args.Get(0) != nil {
		// Set ID to simulate database behavior
		event.ID = 1
	}
	return args.Error(0)
}

func (m *MockEventRepository) FindByName(ctx context.Context, name string) ([]model.Event, error) {
	args := m.Called(ctx, name)
	return args.Get(0).([]model.Event), args.Error(1)
}

func (m *MockEventRepository) FindByIDIn(ctx context.Context, ids []int64) ([]model.Event, error) {
	args := m.Called(ctx, ids)
	return args.Get(0).([]model.Event), args.Error(1)
}

func (m *MockEventRepository) SearchByProperty(ctx context.Context, eventName string, properties model.Properties, operation model.Operation) ([]model.Event, error) {
	args := m.Called(ctx, eventName, properties, operation)
	return args.Get(0).([]model.Event), args.Error(1)
}

// MockCampaignRepository is a mock for CampaignRepository
type MockCampaignRepository struct {
	mock.Mock
}

func (m *MockCampaignRepository) Create(ctx context.Context, campaign *model.Campaign) error {
	args := m.Called(ctx, campaign)
	return args.Error(0)
}

func (m *MockCampaignRepository) FindByName(ctx context.Context, name string) (*model.Campaign, error) {
	args := m.Called(ctx, name)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Campaign), args.Error(1)
}

func (m *MockCampaignRepository) ExistsByName(ctx context.Context, name string) (bool, error) {
	args := m.Called(ctx, name)
	return args.Bool(0), args.Error(1)
}

// MockCampaignEventsRepository is a mock for CampaignEventsRepository
type MockCampaignEventsRepository struct {
	mock.Mock
}

func (m *MockCampaignEventsRepository) Create(ctx context.Context, ce *model.CampaignEvents) error {
	args := m.Called(ctx, ce)
	return args.Error(0)
}

// MockUserRepository is a mock for UserRepository
type MockUserRepository struct {
	mock.Mock
}

func (m *MockUserRepository) FindByExternalID(ctx context.Context, externalID string) (*model.User, error) {
	args := m.Called(ctx, externalID)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.User), args.Error(1)
}

func (m *MockUserRepository) FindByIDIn(ctx context.Context, ids []int64) ([]model.User, error) {
	args := m.Called(ctx, ids)
	return args.Get(0).([]model.User), args.Error(1)
}

func TestEventService_CreateEvent_Success(t *testing.T) {
	// Setup
	mockEventRepo := new(MockEventRepository)
	mockCampaignRepo := new(MockCampaignRepository)
	mockCampaignEventsRepo := new(MockCampaignEventsRepository)
	mockUserRepo := new(MockUserRepository)
	logger := zap.NewNop()

	service := NewEventService(mockEventRepo, mockCampaignRepo, mockCampaignEventsRepo, mockUserRepo, logger)

	ctx := context.Background()
	req := &dto.CreateEventRequest{
		Name:       "purchase",
		ExternalID: "user123",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "laptop"},
		},
	}

	user := &model.User{ID: 1, ExternalID: "user123"}
	mockUserRepo.On("FindByExternalID", ctx, "user123").Return(user, nil)
	mockEventRepo.On("Create", ctx, mock.AnythingOfType("*model.Event")).Return(nil)

	// Execute
	resp, err := service.CreateEvent(ctx, req)

	// Assert
	assert.NoError(t, err)
	assert.NotNil(t, resp)
	assert.Equal(t, int64(1), resp.ID)
	assert.Equal(t, "Event saved successfully", resp.Message)
	mockUserRepo.AssertExpectations(t)
	mockEventRepo.AssertExpectations(t)
}

func TestEventService_CreateEvent_UserNotFound(t *testing.T) {
	// Setup
	mockEventRepo := new(MockEventRepository)
	mockCampaignRepo := new(MockCampaignRepository)
	mockCampaignEventsRepo := new(MockCampaignEventsRepository)
	mockUserRepo := new(MockUserRepository)
	logger := zap.NewNop()

	service := NewEventService(mockEventRepo, mockCampaignRepo, mockCampaignEventsRepo, mockUserRepo, logger)

	ctx := context.Background()
	req := &dto.CreateEventRequest{
		Name:       "purchase",
		ExternalID: "user123",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "laptop"},
		},
	}

	mockUserRepo.On("FindByExternalID", ctx, "user123").Return(nil, nil)

	// Execute
	resp, err := service.CreateEvent(ctx, req)

	// Assert
	assert.Error(t, err)
	assert.Nil(t, resp)
	assert.IsType(t, &apperrors.AppError{}, err)
	appErr := err.(*apperrors.AppError)
	assert.Equal(t, apperrors.ErrCodeUserNotFound, appErr.Code)
	mockUserRepo.AssertExpectations(t)
}

func TestEventService_CreateEvent_WithCampaign_Success(t *testing.T) {
	// Setup
	mockEventRepo := new(MockEventRepository)
	mockCampaignRepo := new(MockCampaignRepository)
	mockCampaignEventsRepo := new(MockCampaignEventsRepository)
	mockUserRepo := new(MockUserRepository)
	logger := zap.NewNop()

	service := NewEventService(mockEventRepo, mockCampaignRepo, mockCampaignEventsRepo, mockUserRepo, logger)

	ctx := context.Background()
	req := &dto.CreateEventRequest{
		Name:         "purchase",
		CampaignName: "summer-sale",
		ExternalID:   "user123",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "laptop"},
		},
	}

	user := &model.User{ID: 1, ExternalID: "user123"}
	campaign := &model.Campaign{
		ID:   1,
		Name: "summer-sale",
		Properties: model.Properties{
			{Key: "product", Value: ""},
		},
	}

	mockUserRepo.On("FindByExternalID", ctx, "user123").Return(user, nil)
	mockEventRepo.On("Create", ctx, mock.AnythingOfType("*model.Event")).Return(nil)
	mockCampaignRepo.On("FindByName", ctx, "summer-sale").Return(campaign, nil)
	mockCampaignEventsRepo.On("Create", ctx, mock.AnythingOfType("*model.CampaignEvents")).Return(nil)

	// Execute
	resp, err := service.CreateEvent(ctx, req)

	// Assert
	assert.NoError(t, err)
	assert.NotNil(t, resp)
	assert.Equal(t, "Event saved with campaign", resp.Message)
	mockUserRepo.AssertExpectations(t)
	mockEventRepo.AssertExpectations(t)
	mockCampaignRepo.AssertExpectations(t)
	mockCampaignEventsRepo.AssertExpectations(t)
}

func TestEventService_CreateEvent_DatabaseError(t *testing.T) {
	// Setup
	mockEventRepo := new(MockEventRepository)
	mockCampaignRepo := new(MockCampaignRepository)
	mockCampaignEventsRepo := new(MockCampaignEventsRepository)
	mockUserRepo := new(MockUserRepository)
	logger := zap.NewNop()

	service := NewEventService(mockEventRepo, mockCampaignRepo, mockCampaignEventsRepo, mockUserRepo, logger)

	ctx := context.Background()
	req := &dto.CreateEventRequest{
		Name:       "purchase",
		ExternalID: "user123",
		Properties: []dto.PropertyDTO{
			{Key: "product", Value: "laptop"},
		},
	}

	user := &model.User{ID: 1, ExternalID: "user123"}
	mockUserRepo.On("FindByExternalID", ctx, "user123").Return(user, nil)
	mockEventRepo.On("Create", ctx, mock.AnythingOfType("*model.Event")).Return(errors.New("database error"))

	// Execute
	resp, err := service.CreateEvent(ctx, req)

	// Assert
	assert.Error(t, err)
	assert.Nil(t, resp)
	mockUserRepo.AssertExpectations(t)
	mockEventRepo.AssertExpectations(t)
}

func TestEventService_SearchEvents_Success(t *testing.T) {
	// Setup
	mockEventRepo := new(MockEventRepository)
	mockCampaignRepo := new(MockCampaignRepository)
	mockCampaignEventsRepo := new(MockCampaignEventsRepository)
	mockUserRepo := new(MockUserRepository)
	logger := zap.NewNop()

	service := NewEventService(mockEventRepo, mockCampaignRepo, mockCampaignEventsRepo, mockUserRepo, logger)

	ctx := context.Background()
	req := &dto.SearchEventsRequest{
		EventName: "purchase",
		Where:     "product&laptop&=&end",
	}

	events := []model.Event{
		{
			ID:     1,
			Name:   "purchase",
			UserID: 1,
			Properties: model.Properties{
				{Key: "product", Value: "laptop"},
			},
			CreatedAt: time.Now(),
		},
	}

	users := []model.User{
		{ID: 1, ExternalID: "user123"},
	}

	mockEventRepo.On("FindByName", ctx, "purchase").Return(events, nil)
	mockUserRepo.On("FindByIDIn", ctx, []int64{1}).Return(users, nil)

	// Execute
	resp, err := service.SearchEvents(ctx, req)

	// Assert
	assert.NoError(t, err)
	assert.NotNil(t, resp)
	assert.Len(t, resp.Events, 1)
	assert.Equal(t, "purchase", resp.Events[0].Name)
	mockEventRepo.AssertExpectations(t)
	mockUserRepo.AssertExpectations(t)
}
