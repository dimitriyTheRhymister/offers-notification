#!/usr/bin/env bash
set -e

echo "=== Applying Kubernetes manifests ==="
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres-offers.yaml
kubectl apply -f k8s/postgres-notification.yaml
kubectl apply -f k8s/offers-deployment.yaml
kubectl apply -f k8s/offers-service.yaml
kubectl apply -f k8s/notification-deployment.yaml
kubectl apply -f k8s/notification-service.yaml
kubectl apply -f k8s/ingress.yaml

echo "=== Waiting for rollout ==="
kubectl -n offers-notification rollout status deploy/postgres-offers
kubectl -n offers-notification rollout status deploy/postgres-notification
kubectl -n offers-notification rollout status deploy/offers-service
kubectl -n offers-notification rollout status deploy/notification-service

echo "=== Done ==="
kubectl get pods -n offers-notification