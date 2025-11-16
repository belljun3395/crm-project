.PHONY: help build run test clean docker-build docker-up docker-down swagger wire

# Variables
APP_NAME=event-service-go
MAIN_PATH=main.go
BINARY_PATH=bin/$(APP_NAME)

# Go parameters
GOCMD=go
GOBUILD=$(GOCMD) build
GOCLEAN=$(GOCMD) clean
GOTEST=$(GOCMD) test
GOMOD=$(GOCMD) mod
WIRE=wire
SWAG=swag

help: ## Display this help screen
	@grep -h -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

deps: ## Download dependencies
	$(GOMOD) download
	$(GOMOD) tidy

wire: ## Generate Wire dependency injection
	$(HOME)/go/bin/wire ./internal/wire

swagger: ## Generate Swagger documentation
	$(HOME)/go/bin/swag init -g main.go --output ./docs

build: deps wire ## Build the application
	mkdir -p bin
	$(GOBUILD) -o $(BINARY_PATH) $(MAIN_PATH)

run: build ## Run the application
	./$(BINARY_PATH)

test: ## Run tests
	$(GOTEST) -v -race -coverprofile=coverage.out ./...

test-coverage: test ## Run tests with coverage report
	$(GOCMD) tool cover -html=coverage.out -o coverage.html
	@echo "Coverage report generated: coverage.html"

clean: ## Clean build artifacts
	$(GOCLEAN)
	rm -rf bin/
	rm -rf docs/
	rm -f coverage.out coverage.html
	rm -f internal/wire/wire_gen.go

docker-build: ## Build Docker image
	docker build -t $(APP_NAME):latest .

docker-up: ## Start services with docker-compose
	docker-compose up -d

docker-down: ## Stop services with docker-compose
	docker-compose down

docker-logs: ## Show docker-compose logs
	docker-compose logs -f

lint: ## Run golangci-lint
	golangci-lint run ./...

fmt: ## Format code
	$(GOCMD) fmt ./...
	goimports -w .

all: clean deps wire swagger build ## Clean, download deps, generate code, and build

dev: ## Run in development mode with hot reload (requires air)
	air

.DEFAULT_GOAL := help
