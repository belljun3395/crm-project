//go:build wireinject
// +build wireinject

package wire

import (
	"event-service-go/internal/config"
	"event-service-go/internal/handler"
	"event-service-go/internal/repository"
	"event-service-go/internal/service"
	"event-service-go/pkg/logger"

	"github.com/google/wire"
	"go.uber.org/zap"
	"gorm.io/gorm"
	gormlogger "gorm.io/gorm/logger"
)

// InitializeApp initializes the application with dependency injection
func InitializeApp(cfg *config.Config) (*App, error) {
	wire.Build(
		// Logger
		provideLogger,

		// Database
		provideDatabase,

		// Redis Cache
		repository.NewRedisCache,

		// Repositories
		repository.NewEventRepository,
		repository.NewCampaignRepository,
		repository.NewCampaignEventsRepository,
		repository.NewUserRepository,

		// Services
		service.NewEventService,
		service.NewCampaignService,

		// Handlers
		handler.NewEventHandlerFromService,
		handler.NewCampaignHandlerFromService,
		handler.NewHealthHandler,

		// App
		NewApp,
	)
	return &App{}, nil
}

// provideLogger provides a zap logger
func provideLogger(cfg *config.Config) (*zap.Logger, error) {
	l, err := logger.NewLogger(cfg.Server.Env)
	if err != nil {
		return nil, err
	}
	return l.Logger, nil
}

// provideDatabase provides a GORM database connection
func provideDatabase(cfg *config.Config, logger *zap.Logger) (*gorm.DB, error) {
	logLevel := gormlogger.Silent
	if cfg.Server.Env == "development" {
		logLevel = gormlogger.Info
	}
	return repository.NewDatabase(cfg.Database, logLevel)
}

// App holds all application dependencies
type App struct {
	EventHandler    *handler.EventHandler
	CampaignHandler *handler.CampaignHandler
	HealthHandler   *handler.HealthHandler
	Logger          *zap.Logger
}

// NewApp creates a new App instance
func NewApp(
	eventHandler *handler.EventHandler,
	campaignHandler *handler.CampaignHandler,
	healthHandler *handler.HealthHandler,
	logger *zap.Logger,
) *App {
	return &App{
		EventHandler:    eventHandler,
		CampaignHandler: campaignHandler,
		HealthHandler:   healthHandler,
		Logger:          logger,
	}
}
