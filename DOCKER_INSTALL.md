# Docker Installation and Usage Guide

## Installing Docker

The Cloud Kitchen Fulfillment System requires Docker to build and run. Follow these steps based on your operating system:

### macOS

1. **Download Docker Desktop**: Visit https://docs.docker.com/desktop/install/mac-install/
2. **Install**: Open the downloaded `.dmg` file and follow the installation wizard
3. **Launch**: Start Docker Desktop from Applications
4. **Verify**: Open Terminal and run `docker --version`

### Linux

For Ubuntu/Debian:
```bash
# Update package index
sudo apt-get update

# Install Docker
sudo apt-get install docker.io

# Add your user to docker group (optional, to run without sudo)
sudo usermod -aG docker $USER

# Log out and log back in, then verify
docker --version
```

For other distributions, visit: https://docs.docker.com/engine/install/

### Windows

1. **Download Docker Desktop**: Visit https://docs.docker.com/desktop/install/windows-install/
2. **Install**: Run the installer and follow the wizard
3. **Restart**: Restart your computer if prompted
4. **Launch**: Start Docker Desktop from the Start menu
5. **Verify**: Open PowerShell or Command Prompt and run `docker --version`

## Building the Application

Once Docker is installed and running:

```bash
cd cloud-kitchen-fulfillment
docker build -t cloud-kitchen-fulfillment .
```

This will:
- Download the Maven and Java 8 base images
- Compile your application
- Package it into a single executable JAR
- Create an optimized runtime image

**First build may take 5-10 minutes** due to downloading dependencies. Subsequent builds are much faster.

## Running the Application

After successful build:

```bash
docker run --rm cloud-kitchen-fulfillment YOUR_AUTH_TOKEN
```

Add `--rm` flag to automatically remove the container after it finishes.

## Troubleshooting

### "Cannot connect to Docker daemon"
- Ensure Docker Desktop is running
- On Linux: Make sure Docker service is started: `sudo systemctl start docker`

### "docker: command not found"
- Docker is not installed or not in your PATH
- Restart your terminal after installation
- On Windows: May need to restart your computer

### Build fails with network errors
- Check your internet connection
- On corporate networks: Configure proxy settings in Docker Desktop

### Permission denied errors (Linux)
- Add your user to docker group: `sudo usermod -aG docker $USER`
- Log out and log back in
- Or run with sudo: `sudo docker build ...`

## Verifying Docker is Working

Run this command to test Docker:

```bash
docker run --rm hello-world
```

If you see "Hello from Docker!", Docker is installed correctly.

