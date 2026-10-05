.PHONY: all deploy build test prism local steamdeck deck clean help

SPYCRAFT_DIR := spycraft
DEPLOY_SCRIPT := scripts/deploy.sh

# Default target: build, install to local Prism Launcher, and sync to Steam Deck
all: deploy

help:
	@echo "🎮 SpyCraft Build & Deploy Targets:"
	@echo "  make            - Build, install to local Prism Launcher & sync to Steam Deck"
	@echo "  make deploy     - Same as make (full pipeline)"
	@echo "  make prism      - Build and install into local macOS Prism Launcher instances"
	@echo "  make local      - Alias for make prism"
	@echo "  make steamdeck  - Build and sync to Steam Deck over SSH"
	@echo "  make deck       - Alias for make steamdeck"
	@echo "  make build      - Run gradle build without tests"
	@echo "  make test       - Run gradle unit tests"
	@echo "  make clean      - Clean gradle build output"

build:
	@echo "🔨 Building SpyCraft mod..."
	@cd $(SPYCRAFT_DIR) && ./gradlew build -x test

test:
	@echo "🧪 Running tests..."
	@cd $(SPYCRAFT_DIR) && ./gradlew test

prism: build
	@./$(DEPLOY_SCRIPT) --local --skip-build

local: prism

steamdeck: build
	@./$(DEPLOY_SCRIPT) --deck --skip-build

deck: steamdeck

deploy: build
	@./$(DEPLOY_SCRIPT) --all --skip-build

clean:
	@echo "🧹 Cleaning build artifacts..."
	@cd $(SPYCRAFT_DIR) && ./gradlew clean
