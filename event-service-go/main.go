package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"event-service-go/internal/api"
	"event-service-go/internal/config"
	"event-service-go/internal/repository"

	"github.com/gin-gonic/gin"
)

func main() {
	// Load configuration
	cfg := config.Load()

	// Initialize database
	db, err := repository.NewDatabase(cfg.Database)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Initialize Redis cache
	cache := repository.NewRedisCache(cfg.Redis)
	defer cache.Close()

	// Initialize repositories
	eventRepo := repository.NewEventRepository(db)
	campaignRepo := repository.NewCampaignRepository(db, cache)
	campaignEventsRepo := repository.NewCampaignEventsRepository(db)
	userRepo := repository.NewUserRepository(db)

	// Initialize handlers
	eventHandler := api.NewEventHandler(eventRepo, campaignRepo, campaignEventsRepo, userRepo)

	// Set up router
	if cfg.Server.Env == "production" {
		gin.SetMode(gin.ReleaseMode)
	}
	router := gin.Default()

	// Health check
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "healthy"})
	})

	// API routes
	v1 := router.Group("/api/v1")
	{
		events := v1.Group("/events")
		{
			events.POST("", eventHandler.PostEvent)
			events.GET("", eventHandler.SearchEvents)
			events.POST("/campaign", eventHandler.PostCampaign)
		}
	}

	// Start server
	srv := &http.Server{
		Addr:    ":" + cfg.Server.Port,
		Handler: router,
	}

	// Graceful shutdown
	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	log.Printf("Server started on port %s", cfg.Server.Port)

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Fatal("Server forced to shutdown:", err)
	}

	log.Println("Server exited")
}
