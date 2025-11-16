package middleware

import (
	"event-service-go/internal/dto"
	"event-service-go/pkg/errors"
	"net/http"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// Recovery middleware recovers from panics and logs them
func Recovery(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				requestID := GetRequestID(c)

				logger.Error("Panic recovered",
					zap.Any("error", err),
					zap.String("request_id", requestID),
					zap.String("path", c.Request.URL.Path),
					zap.Stack("stack"),
				)

				c.JSON(http.StatusInternalServerError, dto.ErrorResponse{
					Success: false,
					Error:   "Internal server error",
					Code:    errors.ErrCodeInternalServer,
				})

				c.Abort()
			}
		}()

		c.Next()
	}
}
