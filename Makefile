.PHONY: help up down logs rebuild clean backend-build backend-test ps

# Default target
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Docker Compose commands
up: ## Start all services
	cd infrastructure/docker && docker compose up -d

down: ## Stop all services
	cd infrastructure/docker && docker compose down

logs: ## Follow logs for all services
	cd infrastructure/docker && docker compose logs -f

logs-backend: ## Follow backend logs only
	cd infrastructure/docker && docker compose logs -f backend

ps: ## Show running services
	cd infrastructure/docker && docker compose ps

rebuild: ## Rebuild and restart all services
	cd infrastructure/docker && docker compose build --no-cache && docker compose up -d

clean: ## Stop services and remove volumes (resets database)
	cd infrastructure/docker && docker compose down -v

# Backend commands
backend-build: ## Build backend (Gradle)
	cd backend && ./gradlew build

backend-test: ## Run backend tests
	cd backend && ./gradlew test

backend-run: ## Run backend locally (needs DB running)
	cd backend && ./gradlew :api:bootRun
