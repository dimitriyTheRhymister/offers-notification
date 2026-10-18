#!/usr/bin/env bash
set -e

echo "=== Building Docker images ==="
docker compose build

echo "=== Loading images into minikube ==="
minikube image load offers-notification-offers-service:latest
minikube image load offers-notification-notification-service:latest

echo "=== Done ==="