#!/bin/bash

# Run script for Perp DEX Tools Java

JAR_FILE="target/perp-dex-tools-1.0.0.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run ./build.sh first to build the project."
    exit 1
fi

# Load environment variables from .env if it exists
if [ -f ".env" ]; then
    echo "Loading environment variables from .env file..."
    set -a
    source .env
    set +a
fi

# Run the application with all arguments passed through
java -jar "$JAR_FILE" "$@"
