#!/usr/bin/env bash
set -euo pipefail

echo "Verifying development environment for Forge AI..."

echo "Java version:" 
java -version || true

if [ -x ./mvnw ]; then
  echo "Using project Maven wrapper"
  ./mvnw -v
else
  echo "Maven wrapper not found. Falling back to system mvn."
  mvn -v
fi

# Run a clean build (skip tests for speed in verification)
if [ -x ./mvnw ]; then
  ./mvnw -B clean verify
else
  mvn -B clean verify
fi

echo "Build succeeded."
