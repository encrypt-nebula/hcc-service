#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
# You can set these environment variables before running the script, 
# or they will use the defaults below.
IMAGE_NAME="hcc-service"
IMAGE_TAG="latest"
IMAGE_TAR_FILE="hcc-service.tar"

# Defaults (Deducted from environment)
DEFAULT_EC2_USER="ubuntu"
DEFAULT_EC2_IP="13.235.138.74"
DEFAULT_KEY_PATH="/Users/harshsharma/Hivemynds/visionnet-dev-ec2.pem"

# --- Setup Paths ---
# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# The project root is one level up from the deployment folder
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Change workdir to project root
cd "$PROJECT_ROOT"

# --- Runtime Inputs ---
# Allow passing arguments: ./mac-deploy.sh [EC2_USER] [EC2_IP] [KEY_PATH]
EC2_USER=${1:-$DEFAULT_EC2_USER}
EC2_IP=${2:-$DEFAULT_EC2_IP}
KEY_PATH=${3:-$DEFAULT_KEY_PATH}

if [[ -z "$1" && -z "$2" && -z "$3" ]]; then
    echo "--- Deployment Configuration ---"
    read -p "Enter EC2 username (default: $DEFAULT_EC2_USER): " EC2_USER
    EC2_USER=${EC2_USER:-$DEFAULT_EC2_USER}

    read -p "Enter EC2 public IP (default: $DEFAULT_EC2_IP): " EC2_IP
    EC2_IP=${EC2_IP:-$DEFAULT_EC2_IP}

    read -p "Enter full path to your PEM key file (default: $DEFAULT_KEY_PATH): " KEY_PATH
    KEY_PATH=${KEY_PATH:-$DEFAULT_KEY_PATH}
fi

if [[ ! -f "$KEY_PATH" ]]; then
    echo "Error: Key file not found at $KEY_PATH"
    exit 1
fi

# --- Build Process ---
echo "--- Building JAR file from latest code ---"
mvn clean install -DskipTests=true

echo "--- Building Docker image (for linux/amd64) ---"
# --platform is critical for Apple Silicon (Mac M1/M2/M3) to work on EC2
docker build --platform linux/amd64 -t $IMAGE_NAME:$IMAGE_TAG .

echo "--- Saving Docker image to tar file ---"
docker save -o $IMAGE_TAR_FILE $IMAGE_NAME:$IMAGE_TAG

# --- Transfer & Deploy ---
echo "--- Transferring image to EC2 instance ($EC2_IP) ---"
scp -i "$KEY_PATH" "$IMAGE_TAR_FILE" "$EC2_USER@$EC2_IP:/home/$EC2_USER"

echo "--- Loading Docker image and restarting container on EC2 ---"
ssh -T -i "$KEY_PATH" "$EC2_USER@$EC2_IP" << EOF
    set -e
    echo "Loading image..."
    sudo docker load -i /home/$EC2_USER/$IMAGE_TAR_FILE
    
    echo "Stopping existing container (if any)..."
    sudo docker rm -f $IMAGE_NAME || true
    
    echo "Starting new container..."
    sudo docker run -d -p 9001:8080 --name $IMAGE_NAME --restart unless-stopped $IMAGE_NAME:$IMAGE_TAG
    
    echo "Cleaning up old images..."
    sudo docker image prune -f
    
    echo "Deleting transferred tar file..."
    rm -f /home/$EC2_USER/$IMAGE_TAR_FILE
EOF

# --- Clean Up ---
echo "--- Cleaning up local tar file ---"
rm -f "$IMAGE_TAR_FILE"

echo "Deployment complete!"
