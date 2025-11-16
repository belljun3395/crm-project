package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"event-service-go/internal/config"

	"github.com/redis/go-redis/v9"
)

// RedisCache handles Redis cluster caching operations
type RedisCache struct {
	client *redis.ClusterClient
}

// NewRedisCache creates a new Redis cache instance
func NewRedisCache(cfg *config.Config) *RedisCache {
	client := redis.NewClusterClient(&redis.ClusterOptions{
		Addrs:    cfg.Redis.Nodes,
		Password: cfg.Redis.Password,
	})

	return &RedisCache{
		client: client,
	}
}

// Set stores a value in the cache with a TTL
func (c *RedisCache) Set(ctx context.Context, key string, value interface{}, ttl time.Duration) error {
	data, err := json.Marshal(value)
	if err != nil {
		return fmt.Errorf("failed to marshal value: %w", err)
	}

	return c.client.Set(ctx, key, data, ttl).Err()
}

// Get retrieves a value from the cache and unmarshals it into the provided pointer
func (c *RedisCache) Get(ctx context.Context, key string, dest interface{}) error {
	data, err := c.client.Get(ctx, key).Bytes()
	if err != nil {
		return err
	}

	if err := json.Unmarshal(data, dest); err != nil {
		return fmt.Errorf("failed to unmarshal value: %w", err)
	}

	return nil
}

// Delete removes a value from the cache
func (c *RedisCache) Delete(ctx context.Context, key string) error {
	return c.client.Del(ctx, key).Err()
}

// Close closes the Redis connection
func (c *RedisCache) Close() error {
	return c.client.Close()
}

// CampaignCacheKey generates a cache key for campaign data
func CampaignCacheKey(prefix, value string) string {
	return fmt.Sprintf("campaign:%s:%s", prefix, value)
}
