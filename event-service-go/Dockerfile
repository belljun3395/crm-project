# Build stage
FROM golang:1.23-alpine AS builder

# Install dependencies for building
RUN apk add --no-cache git make

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Install wire and swag
RUN go install github.com/google/wire/cmd/wire@latest
RUN go install github.com/swaggo/swag/cmd/swag@latest

# Generate wire code and swagger docs
RUN wire ./internal/wire
RUN swag init

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o event-service-go main.go

# Final stage
FROM alpine:latest

RUN apk --no-cache add ca-certificates tzdata

WORKDIR /root/

# Copy binary from builder
COPY --from=builder /app/event-service-go .
COPY --from=builder /app/docs ./docs

# Expose port
EXPOSE 8081

# Run the binary
CMD ["./event-service-go"]
