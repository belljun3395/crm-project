package handler

import (
	"net/http"

	"event-service-go/internal/dto"
	"event-service-go/internal/service"
	apperrors "event-service-go/pkg/errors"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// CampaignHandler handles HTTP requests for campaigns
type CampaignHandler struct {
	campaignService CampaignServiceInterface
	logger          *zap.Logger
}

// NewCampaignHandler creates a new campaign handler
func NewCampaignHandler(campaignService CampaignServiceInterface, logger *zap.Logger) *CampaignHandler {
	return &CampaignHandler{
		campaignService: campaignService,
		logger:          logger,
	}
}

// NewCampaignHandlerFromService creates a new campaign handler from concrete service
func NewCampaignHandlerFromService(campaignService *service.CampaignService, logger *zap.Logger) *CampaignHandler {
	return NewCampaignHandler(campaignService, logger)
}

// CreateCampaign godoc
// @Summary      Create a new campaign
// @Description  Create a new campaign with property definitions
// @Tags         campaigns
// @Accept       json
// @Produce      json
// @Param        request body dto.CreateCampaignRequest true "Campaign details"
// @Success      201 {object} dto.SuccessResponse{data=dto.CreateCampaignResponse}
// @Failure      400 {object} dto.ErrorResponse
// @Failure      409 {object} dto.ErrorResponse
// @Failure      500 {object} dto.ErrorResponse
// @Router       /api/v2/events/campaign [post]
func (h *CampaignHandler) CreateCampaign(c *gin.Context) {
	var req dto.CreateCampaignRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.respondWithError(c, apperrors.NewValidationError("Invalid request body", err))
		return
	}

	resp, err := h.campaignService.CreateCampaign(c.Request.Context(), &req)
	if err != nil {
		h.respondWithError(c, err)
		return
	}

	c.JSON(http.StatusCreated, dto.SuccessResponse{
		Success: true,
		Data:    resp,
	})
}

// respondWithError handles error responses
func (h *CampaignHandler) respondWithError(c *gin.Context, err error) {
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
