package handler

import (
	"net/http"

	"event-service-go/internal/dto"
	"event-service-go/internal/service"
	apperrors "event-service-go/pkg/errors"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// EventHandler handles HTTP requests for events
type EventHandler struct {
	eventService *service.EventService
	logger       *zap.Logger
}

// NewEventHandler creates a new event handler
func NewEventHandler(eventService *service.EventService, logger *zap.Logger) *EventHandler {
	return &EventHandler{
		eventService: eventService,
		logger:       logger,
	}
}

// CreateEvent godoc
// @Summary      Create a new event
// @Description  Create a new event with optional campaign association
// @Tags         events
// @Accept       json
// @Produce      json
// @Param        request body dto.CreateEventRequest true "Event details"
// @Success      201 {object} dto.SuccessResponse{data=dto.CreateEventResponse}
// @Failure      400 {object} dto.ErrorResponse
// @Failure      404 {object} dto.ErrorResponse
// @Failure      500 {object} dto.ErrorResponse
// @Router       /api/v2/events [post]
func (h *EventHandler) CreateEvent(c *gin.Context) {
	var req dto.CreateEventRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.respondWithError(c, apperrors.NewValidationError("Invalid request body", err))
		return
	}

	resp, err := h.eventService.CreateEvent(c.Request.Context(), &req)
	if err != nil {
		h.respondWithError(c, err)
		return
	}

	c.JSON(http.StatusCreated, dto.SuccessResponse{
		Success: true,
		Data:    resp,
	})
}

// SearchEvents godoc
// @Summary      Search events
// @Description  Search events by name and property filters
// @Tags         events
// @Accept       json
// @Produce      json
// @Param        eventName query string true "Event name"
// @Param        where query string true "Search filter (format: key&value&operation&joinOperation)"
// @Success      200 {object} dto.SuccessResponse{data=dto.SearchEventsResponse}
// @Failure      400 {object} dto.ErrorResponse
// @Failure      500 {object} dto.ErrorResponse
// @Router       /api/v2/events [get]
func (h *EventHandler) SearchEvents(c *gin.Context) {
	var req dto.SearchEventsRequest
	if err := c.ShouldBindQuery(&req); err != nil {
		h.respondWithError(c, apperrors.NewValidationError("Invalid query parameters", err))
		return
	}

	resp, err := h.eventService.SearchEvents(c.Request.Context(), &req)
	if err != nil {
		h.respondWithError(c, err)
		return
	}

	c.JSON(http.StatusOK, dto.SuccessResponse{
		Success: true,
		Data:    resp,
	})
}

// respondWithError handles error responses
func (h *EventHandler) respondWithError(c *gin.Context, err error) {
	if appErr, ok := err.(*apperrors.AppError); ok {
		h.logger.Warn("Application error",
			zap.String("code", appErr.Code),
			zap.String("message", appErr.Message),
			zap.Error(appErr.Err))

		c.JSON(appErr.StatusCode, dto.ErrorResponse{
			Success: false,
			Error:   appErr.Message,
			Code:    appErr.Code,
		})
		return
	}

	h.logger.Error("Unexpected error", zap.Error(err))
	c.JSON(http.StatusInternalServerError, dto.ErrorResponse{
		Success: false,
		Error:   "Internal server error",
		Code:    apperrors.ErrCodeInternalServer,
	})
}
