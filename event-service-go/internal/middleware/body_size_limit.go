package middleware

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// BodySizeLimit middleware limits the size of request bodies
func BodySizeLimit(maxSize int64) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Set max bytes reader
		c.Request.Body = http.MaxBytesReader(c.Writer, c.Request.Body, maxSize)

		// Try to read the body
		if err := c.Request.ParseForm(); err != nil {
			// Check if error is due to body size
			if err.Error() == "http: request body too large" {
				c.JSON(http.StatusRequestEntityTooLarge, gin.H{
					"success": false,
					"error": gin.H{
						"code":    "REQUEST_TOO_LARGE",
						"message": "Request body too large",
					},
				})
				c.Abort()
				return
			}
		}

		c.Next()
	}
}
