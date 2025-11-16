package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"event-service-go/internal/config"

	"github.com/redis/go-redis/v9"
)

type RedisCache struct {
	client *redis.ClusterClient
}

func NewRedisCache(cfg config.RedisConfig) *RedisCache {
	client := redis.NewClusterClient(&redis.ClusterOptions{
		Addrs:    cfg.Addresses,
		Password: cfg.Password,
	})

	return &RedisCache{client: client}
}

func (r *RedisCache) Get(ctx context.Context, key string, dest interface{}) error {
	val, err := r.client.Get(ctx, key).Result()
	if err != nil {
		return err
	}
	return json.Unmarshal([]byte(val), dest)
}

func (r *RedisCache) Set(ctx context.Context, key string, value interface{}, ttl time.Duration) error {
	data, err := json.Marshal(value)
	if err != nil {
		return err
	}
	return r.client.Set(ctx, key, data, ttl).Err()
}

func (r *RedisCache) Delete(ctx context.Context, key string) error {
	return r.client.Del(ctx, key).Err()
}

func (r *RedisCache) Close() error {
	return r.client.Close()
}

// CampaignCacheKey generates cache key for campaign
func CampaignCacheKey(field, value string) string {
	return fmt.Sprintf("campaign:%s:%s", field, value)
}
