#!/bin/bash

# Ensure the script exits if any command fails
set -e

# Variables
IMAGE_NAME="hcc-service"         # Docker image name for HCC
IMAGE_TAG="latest"                # Docker image tag
IMAGE_TAR_FILE="hcc-service.tar"

# Default Variables
DEFAULT_USER="ubuntu"
DEFAULT_IP="54.84.217.140"
DEFAULT_KEY="/Users/harshsharma/Hivemynds/hcc-service/deployment/hcc-keypair.pem"

# Move to the project root directory
cd "$(dirname "$0")/.."

# Runtime inputs with defaults
read -p "Enter EC2 username [$DEFAULT_USER]: " EC2_USER
EC2_USER=${EC2_USER:-$DEFAULT_USER}

read -p "Enter EC2 public IP [$DEFAULT_IP]: " EC2_IP
EC2_IP=${EC2_IP:-$DEFAULT_IP}

read -p "Enter full path to your PEM key file [$DEFAULT_KEY]: " KEY_PATH
KEY_PATH=${KEY_PATH:-$DEFAULT_KEY}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
  echo "Docker is not running. Please start Docker Desktop and try again."
  exit 1
fi

# Build the jar file
echo "Building jar file from latest code..."
mvn clean install -DskipTests=true

# Build the Docker image locally
echo "Building Docker image..."
docker build --platform linux/amd64 -t $IMAGE_NAME:$IMAGE_TAG .

# Save the Docker image to a tar file
echo "Saving Docker image to tar file..."
docker save -o $IMAGE_TAR_FILE $IMAGE_NAME:$IMAGE_TAG

# Ensure key has correct permissions
chmod 600 "$KEY_PATH"

# Transfer the Docker image tar file to EC2
echo "Transferring image to EC2 instance..."
scp -i "$KEY_PATH" "$IMAGE_TAR_FILE" "$EC2_USER@$EC2_IP:/home/$EC2_USER/"

# SSH into EC2 and load the Docker image
echo "Loading Docker image on EC2..."
ssh -T -i "$KEY_PATH" "$EC2_USER@$EC2_IP" << EOF
    sudo docker load -i /home/$EC2_USER/$IMAGE_TAR_FILE
    sudo docker rm -f $IMAGE_NAME || true
    # Run on port 8080 as per deploy.sh
    sudo docker run -d -p 8080:8080 --name $IMAGE_NAME --restart unless-stopped $IMAGE_NAME:$IMAGE_TAG
    sudo docker image prune -f
EOF

# Clean up locally
echo "Cleaning up local tar file..."
rm -f "$IMAGE_TAR_FILE"

echo "Deployment complete!"
