package middleware

import (
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// Logger middleware logs HTTP requests using zap
func Logger(logger *zap.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		query := c.Request.URL.RawQuery

		// Process request
		c.Next()

		// Calculate latency
		latency := time.Since(start)

		// Get request ID
		requestID := GetRequestID(c)

		// Log request
		fields := []zap.Field{
			zap.Int("status", c.Writer.Status()),
			zap.String("method", c.Request.Method),
			zap.String("path", path),
			zap.String("query", query),
			zap.String("ip", c.ClientIP()),
			zap.String("user_agent", c.Request.UserAgent()),
			zap.Duration("latency", latency),
			zap.String("request_id", requestID),
		}

		// Add error if exists
		if len(c.Errors) > 0 {
			fields = append(fields, zap.String("errors", c.Errors.String()))
		}

		// Log based on status code
		if c.Writer.Status() >= 500 {
			logger.Error("Server error", fields...)
		} else if c.Writer.Status() >= 400 {
			logger.Warn("Client error", fields...)
		} else {
			logger.Info("Request processed", fields...)
		}
	}
}
