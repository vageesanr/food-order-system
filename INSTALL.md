# Quick Start Guide

## Installation Steps

1. **Install Docker** (if not already installed)
   - See [DOCKER_INSTALL.md](DOCKER_INSTALL.md) for detailed instructions
   - Verify installation: `docker --version`

2. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd cloud-kitchen-fulfillment
   docker build -t cloud-kitchen-fulfillment .
   ```

3. **Run**
   ```bash
   docker run --rm cloud-kitchen-fulfillment YOUR_AUTH_TOKEN
   ```

## Troubleshooting

### Docker not found
```bash
# Install Docker first - see DOCKER_INSTALL.md
```

### Build fails
```bash
# Check internet connection
# Try building again (first build downloads dependencies)
docker build --no-cache -t cloud-kitchen-fulfillment .
```

### Permission errors (Linux)
```bash
# Add user to docker group
sudo usermod -aG docker $USER
# Log out and log back in
```

### Run fails with authentication error
- Ensure you have a valid auth token from your recruiter
- Run: `docker run --rm cloud-kitchen-fulfillment YOUR_AUTH_TOKEN`

## Example Usage

```bash
# Default settings (500ms rate, 4-8s pickup window)
docker run --rm cloud-kitchen-fulfillment abc123_token

# Custom rate and pickup times
docker run --rm cloud-kitchen-fulfillment abc123_token 1000 3000 6000

# With specific seed for testing
docker run --rm cloud-kitchen-fulfillment abc123_token 500 4000 8000 12345
```

## Next Steps

- Read [README.md](README.md) for detailed information
- Review [DOCKER_INSTALL.md](DOCKER_INSTALL.md) for Docker setup
- See README.md for discard strategy explanation

