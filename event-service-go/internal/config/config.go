package config

import (
	"os"
)

// Config holds all application configuration
type Config struct {
	Server   ServerConfig
	Database DatabaseConfig
	Redis    RedisConfig
	Logger   LoggerConfig
}

// ServerConfig holds server configuration
type ServerConfig struct {
	Port string
	Env  string
}

// DatabaseConfig holds database configuration
type DatabaseConfig struct {
	Host     string
	Port     string
	User     string
	Password string
	Name     string
}

// RedisConfig holds Redis cluster configuration
type RedisConfig struct {
	Nodes    []string
	Password string
}

// LoggerConfig holds logger configuration
type LoggerConfig struct {
	Level string
}

// Load loads configuration from environment variables
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
			Name:     getEnv("DB_NAME", "crm"),
		},
		Redis: RedisConfig{
			Nodes: []string{
				getEnv("REDIS_NODE_1", "localhost:7001"),
				getEnv("REDIS_NODE_2", "localhost:7002"),
				getEnv("REDIS_NODE_3", "localhost:7003"),
				getEnv("REDIS_NODE_4", "localhost:7004"),
				getEnv("REDIS_NODE_5", "localhost:7005"),
				getEnv("REDIS_NODE_6", "localhost:7006"),
			},
			Password: getEnv("REDIS_PASSWORD", ""),
		},
		Logger: LoggerConfig{
			Level: getEnv("LOG_LEVEL", "info"),
		},
	}
}

// getEnv retrieves an environment variable or returns a default value
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
