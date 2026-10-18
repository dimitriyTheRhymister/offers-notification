#!/usr/bin/env bash
set -e

echo "=== Deleting namespace ==="
kubectl delete namespace offers-notification --ignore-not-found

echo "=== Done ==="