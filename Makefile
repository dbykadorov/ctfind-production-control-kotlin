SHELL := /bin/bash

GRADLEW := ./gradlew
FRONTEND_DIR := frontend/cabinet
COMPOSE := docker compose
HEALTH_URL := http://localhost:8080/actuator/health

.DEFAULT_GOAL := help

.PHONY: help \
	backend-test backend-test-docker backend-build backend-run \
	frontend-install frontend-test frontend-build \
	test build check \
	docker-up docker-up-detached docker-down docker-reset docker-ps \
	logs-app logs-frontend logs-postgres health

help: ## Show available commands
	@echo "Usage: make <target>"
	@echo ""
	@echo "Common targets:"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z0-9_-]+:.*##/ {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

backend-test: ## Run backend tests
	$(GRADLEW) test

backend-test-docker: ## Run backend tests inside gradle:9.4.1-jdk21 (no wrapper download)
	docker run --rm \
		-e GRADLE_USER_HOME=/tmp/gradle-home \
		-v "$(CURDIR)":/workspace \
		-w /workspace \
		gradle:9.4.1-jdk21 \
		gradle --project-cache-dir /tmp/gradle-project-cache test

backend-build: ## Build backend artifacts
	$(GRADLEW) build

backend-run: ## Run backend app locally
	$(GRADLEW) bootRun

frontend-install: ## Install frontend dependencies
	cd $(FRONTEND_DIR) && pnpm install

frontend-test: ## Run frontend tests
	cd $(FRONTEND_DIR) && pnpm test

frontend-build: ## Build frontend
	cd $(FRONTEND_DIR) && pnpm build

test: backend-test frontend-test ## Run all tests

build: backend-build frontend-build ## Build backend and frontend

check: test ## Alias for full local checks

docker-up: ## Start local stack with rebuild
	$(COMPOSE) up --build

docker-up-detached: ## Start local stack in detached mode
	$(COMPOSE) up --build --wait

docker-down: ## Stop local stack, keep volumes
	$(COMPOSE) down

docker-reset: ## Stop local stack and delete volumes
	$(COMPOSE) down -v

docker-ps: ## Show local stack status
	$(COMPOSE) ps

logs-app: ## Follow backend logs
	$(COMPOSE) logs -f app

logs-frontend: ## Follow frontend logs
	$(COMPOSE) logs -f frontend

logs-postgres: ## Follow database logs
	$(COMPOSE) logs -f postgres

health: ## Check backend health endpoint
	curl --fail --silent --show-error $(HEALTH_URL)
	@echo ""
