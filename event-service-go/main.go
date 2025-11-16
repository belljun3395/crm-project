package main

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"event-service-go/internal/config"
	"event-service-go/internal/middleware"
	"event-service-go/internal/wire"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
	"go.uber.org/zap"
)

// @title           Event Service API
// @version         2.0
// @description     Event Service API for CRM system - Enterprise Edition
// @termsOfService  http://swagger.io/terms/

// @contact.name   API Support
// @contact.email  support@example.com

// @license.name  Apache 2.0
// @license.url   http://www.apache.org/licenses/LICENSE-2.0.html

// @host      localhost:8081
// @BasePath  /

// @securityDefinitions.apikey ApiKeyAuth
// @in header
// @name Authorization
func main() {
	// Load configuration
	cfg := config.Load()

	// Initialize app with dependency injection
	app, err := wire.InitializeApp(cfg)
	if err != nil {
		fmt.Printf("Failed to initialize app: %v\n", err)
		os.Exit(1)
	}
	defer app.Logger.Sync()

	app.Logger.Info("Starting Event Service",
		zap.String("env", cfg.Server.Env),
		zap.String("port", cfg.Server.Port))

	// Set up router
	if cfg.Server.Env == "production" {
		gin.SetMode(gin.ReleaseMode)
	}

	router := gin.New() // Don't use Default() as we add custom middleware

	// Add middleware
	router.Use(middleware.RequestID())
	router.Use(middleware.Logger(app.Logger))
	router.Use(middleware.Recovery(app.Logger))
	router.Use(middleware.CORS())
	router.Use(middleware.RateLimit(100, 200))             // 100 requests/second with burst of 200
	router.Use(middleware.BodySizeLimit(10 * 1024 * 1024)) // 10MB max body size

	// Swagger documentation
	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))

	// Health check
	router.GET("/health", app.HealthHandler.HealthCheck)

	// API v2 routes
	v2 := router.Group("/api/v2")
	{
		events := v2.Group("/events")
		{
			events.POST("", app.EventHandler.CreateEvent)
			events.GET("", app.EventHandler.SearchEvents)
			events.POST("/campaign", app.CampaignHandler.CreateCampaign)
		}
	}

	// Create HTTP server
	srv := &http.Server{
		Addr:         ":" + cfg.Server.Port,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server in a goroutine
	go func() {
		app.Logger.Info("Server listening", zap.String("port", cfg.Server.Port))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			app.Logger.Fatal("Failed to start server", zap.Error(err))
		}
	}()

	// Wait for interrupt signal for graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	app.Logger.Info("Shutting down server...")

	// Graceful shutdown with 5 second timeout
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		app.Logger.Error("Server forced to shutdown", zap.Error(err))
	}

	app.Logger.Info("Server exited")
}
