#!/bin/bash

# Build script for Perp DEX Tools Java

set -e

echo "======================================"
echo "Building Perp DEX Tools (Java)"
echo "======================================"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven 3.8+ first."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F'.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Error: Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "Java version: $JAVA_VERSION ✓"
echo "Maven version: $(mvn -version | head -n 1)"
echo ""

# Clean and build
echo "Cleaning previous builds..."
mvn clean

echo ""
echo "Building project..."
mvn package -DskipTests

echo ""
echo "======================================"
echo "Build completed successfully!"
echo "======================================"
echo ""
echo "Executable JAR location:"
echo "  target/perp-dex-tools-1.0.0.jar"
echo ""
echo "To run the application:"
echo "  ./run.sh --help"
echo ""
