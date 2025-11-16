package config

import (
	"os"
)

type Config struct {
	Server   ServerConfig
	Database DatabaseConfig
	Redis    RedisConfig
}

type ServerConfig struct {
	Port string
	Env  string
}

type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	DBName   string
}

type RedisConfig struct {
	Addresses []string
	Password  string
}

func Load() *Config {
	return &Config{
		Server: ServerConfig{
			Port: getEnv("SERVER_PORT", "8081"),
			Env:  getEnv("ENV", "development"),
		},
		Database: DatabaseConfig{
			Host:     getEnv("DB_HOST", "localhost"),
			Port:     getEnv("DB_PORT", "13306"),
			User:     getEnv("DB_USER", "root"),
			Password: getEnv("DB_PASSWORD", "root"),
			DBName:   getEnv("DB_NAME", "crm"),
		},
		Redis: RedisConfig{
			Addresses: []string{
				getEnv("REDIS_NODE_1", "localhost:7001"),
				getEnv("REDIS_NODE_2", "localhost:7002"),
				getEnv("REDIS_NODE_3", "localhost:7003"),
				getEnv("REDIS_NODE_4", "localhost:7004"),
				getEnv("REDIS_NODE_5", "localhost:7005"),
				getEnv("REDIS_NODE_6", "localhost:7006"),
			},
			Password: getEnv("REDIS_PASSWORD", "password"),
		},
	}
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
