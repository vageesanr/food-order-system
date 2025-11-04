# Cloud Kitchen Fulfillment System

A real-time system for fulfillment of food orders in a delivery-only kitchen. The system handles concurrent order placement, storage management, and pickup operations with intelligent discard strategies.

## Features

- **Real-time Order Processing**: Handles concurrent order placement and pickup operations
- **Intelligent Storage Management**: Automatically places orders in optimal storage locations (heater, cooler, shelf)
- **Efficient Discard Strategy**: Uses priority queues for O(log n) discard operations
- **Freshness Tracking**: Monitors food freshness with temperature-based degradation
- **Challenge Server Integration**: Fetches test problems and submits solutions automatically

## Architecture

### Core Components

- **StorageManager**: Thread-safe storage system managing heater (6 slots), cooler (6 slots), and shelf (12 slots)
- **DiscardStrategy**: Efficient O(log n) discard algorithm using priority queues
- **KitchenService**: Coordinates concurrent order operations
- **ChallengeApiClient**: Communicates with the challenge server

### Storage Logic

1. **Ideal Placement**: Orders are first placed in their ideal temperature storage (hot→heater, cold→cooler, room→shelf)
2. **Shelf Fallback**: If ideal storage is full, orders go to the shelf
3. **Smart Movement**: When shelf is full, cold/hot orders are moved to cooler/heater if space is available
4. **Intelligent Discard**: When no movement is possible, the least fresh order is discarded

## Discard Strategy

The system uses a sophisticated discard strategy that prioritizes food freshness and value:

### Algorithm
- **Priority Queues**: Each storage type maintains a priority queue ordered by freshness value
- **Freshness Calculation**: `freshness = max(0, (ideal_freshness - effective_age) / ideal_freshness)`
- **Degradation Rate**: Orders not at ideal temperature degrade 2x faster
- **Selection Criteria**: Orders with the lowest freshness value are discarded first

### Why This Approach?
1. **Efficiency**: O(log n) time complexity for discard operations using priority queues
2. **Fairness**: Considers both time and storage conditions when calculating freshness
3. **Value Preservation**: Prioritizes keeping fresher, more valuable orders
4. **Temperature Awareness**: Accounts for accelerated degradation in non-ideal storage

## Build Instructions

### Prerequisites
- **Docker** (required for running the application)
  - Install from: https://docs.docker.com/get-docker/
  - After installation, verify with: `docker --version`
  - **Detailed installation instructions**: See [DOCKER_INSTALL.md](DOCKER_INSTALL.md)

### Building the Application

The application is dockerized for easy deployment. Build and run using Docker:

```bash
# Clone the repository
git clone <repository-url>
cd cloud-kitchen-fulfillment

# Build the Docker image
docker build -t cloud-kitchen-fulfillment .
```

The Docker build process will:
1. Download Maven and Java 8 inside the container
2. Compile the application with all dependencies
3. Package everything into a single JAR file
4. Create a minimal runtime image with just the JAR

## Running the Application

After building the Docker image, run the application using Docker:

### Command Line Usage

```bash
docker run --rm cloud-kitchen-fulfillment <auth_token> [rate_ms] [min_pickup_ms] [max_pickup_ms] [seed]
```

### Parameters

- **auth_token** (required): Authentication token for the challenge server
- **rate_ms** (optional): Order placement rate in milliseconds (default: 500)
- **min_pickup_ms** (optional): Minimum pickup time in milliseconds (default: 4000)
- **max_pickup_ms** (optional): Maximum pickup time in milliseconds (default: 8000)
- **seed** (optional): Seed for reproducible test problems

### Examples

```bash
# Basic usage with default parameters
docker run --rm cloud-kitchen-fulfillment your_auth_token

# Custom rate and pickup times
docker run --rm cloud-kitchen-fulfillment your_auth_token 1000 3000 6000

# With specific seed for reproducible testing
docker run --rm cloud-kitchen-fulfillment your_auth_token 500 4000 8000 12345

# View help
docker run --rm cloud-kitchen-fulfillment
```

## Output

The system provides real-time logging of all kitchen actions:

```
14:23:45.123 [main] INFO  c.c.s.StorageManager - Placing order: Order{id='abc123', name='Cheese Pizza', temp=hot, price=10.00, freshness=120s}
14:23:45.124 [main] INFO  c.c.s.StorageManager - Placed order abc123 in heater
14:23:49.456 [pool-1-thread-2] INFO  c.c.s.StorageManager - Picking up order: abc123
14:23:49.457 [pool-1-thread-2] INFO  c.c.s.StorageManager - Picked up order abc123 from heater
```

## Testing

The system automatically:
1. Fetches test problems from the challenge server
2. Processes orders according to the specified rate and pickup intervals
3. Submits the action ledger to the challenge server
4. Displays the test result

## Concurrency

The system is designed for concurrent operation:
- **Thread-safe Storage**: Uses ReadWriteLock for efficient concurrent access
- **Async Operations**: Order placement and pickup operations are asynchronous
- **Scheduled Pickups**: Uses ScheduledExecutorService for timed pickup operations
- **Action Ledger**: Thread-safe collection of all kitchen actions

## Performance Characteristics

- **Storage Operations**: O(1) average case for placement and pickup
- **Discard Operations**: O(log n) worst case using priority queues
- **Concurrent Access**: Efficient read/write locking for high throughput
- **Memory Usage**: Minimal overhead with efficient data structures

## Error Handling

The system includes comprehensive error handling:
- **Network Failures**: Retries and proper error reporting for API communication
- **Concurrency Issues**: Proper locking and exception handling
- **Invalid Operations**: Graceful handling of edge cases (missing orders, full storage)
- **Resource Cleanup**: Proper shutdown of thread pools and resources
