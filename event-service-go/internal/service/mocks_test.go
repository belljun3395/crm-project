package service

import (
	"context"

	"event-service-go/internal/model"

	"github.com/stretchr/testify/mock"
)

// MockEventRepository is a mock for EventRepository
type MockEventRepository struct {
	mock.Mock
}

func (m *MockEventRepository) Create(ctx context.Context, event *model.Event) error {
	args := m.Called(ctx, event)
	if args.Error(0) == nil {
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
