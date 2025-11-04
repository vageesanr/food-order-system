#!/bin/bash

# Cloud Kitchen Fulfillment System Run Script
# Usage: ./run.sh <auth_token> [rate_ms] [min_pickup_ms] [max_pickup_ms] [seed]

if [ $# -lt 1 ]; then
    echo "Usage: $0 <auth_token> [rate_ms] [min_pickup_ms] [max_pickup_ms] [seed]"
    echo "  auth_token: Authentication token for the challenge server"
    echo "  rate_ms: Order placement rate in milliseconds (default: 500)"
    echo "  min_pickup_ms: Minimum pickup time in milliseconds (default: 4000)"
    echo "  max_pickup_ms: Maximum pickup time in milliseconds (default: 8000)"
    echo "  seed: Optional seed for reproducible test problems"
    exit 1
fi

# Build classpath
CLASSPATH="target/classes"
if command -v mvn &> /dev/null; then
    DEPENDENCIES=$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout 2>/dev/null)
    if [ $? -eq 0 ]; then
        CLASSPATH="$CLASSPATH:$DEPENDENCIES"
    fi
fi

# Run the application
echo "Starting Cloud Kitchen Fulfillment System..."
echo "Classpath: $CLASSPATH"
echo "Arguments: $@"
echo ""

java -cp "$CLASSPATH" com.cloudkitchens.Main "$@"

