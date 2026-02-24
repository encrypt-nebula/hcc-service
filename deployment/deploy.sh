#!/bin/bash

# Variables
IMAGE_NAME="hcc-service"
IMAGE_TAG="latest"
IMAGE_TAR_FILE="hcc-service.tar"

#KEY_PATH="/Users/abhishekgupta/Desktop/hcc/hcc-keypair.pem"
#EC2_IP="3.88.1.70"
#EC2_USER=ubuntu

# Runtime inputs
read -p "Enter EC2 username (e.g., ubuntu): " EC2_USER
read -p "Enter EC2 public IP: " EC2_IP
read -p "Enter full path to your PEM key file: " KEY_PATH


# Build the jar
echo "Building jar file from latest code..."
mvn clean install -DskipTests=true

# Build Docker image locally
echo "Building Docker image..."
docker build --platform linux/amd64 -t $IMAGE_NAME:$IMAGE_TAG .

# Save Docker image to tar file
echo "Saving Docker image to tar file..."
docker save -o $IMAGE_TAR_FILE $IMAGE_NAME:$IMAGE_TAG

# Create directory on EC2 and transfer the tar
echo "Transferring image to EC2 instance..."
scp -i "$KEY_PATH" "$IMAGE_TAR_FILE" "$EC2_USER@$EC2_IP:/home/$EC2_USER"

# Load Docker image and restart container
echo "Loading Docker image on EC2..."
ssh -T -i "$KEY_PATH" "$EC2_USER@$EC2_IP" << EOF
    sudo docker load -i /home/$EC2_USER/$IMAGE_TAR_FILE
    sudo docker rm -f $IMAGE_NAME || true
    sudo docker run -d -p 8080:8080 --name $IMAGE_NAME --restart unless-stopped $IMAGE_NAME:$IMAGE_TAG
    sudo docker image prune -f
EOF

# Clean up local tar
rm -f "$IMAGE_TAR_FILE"

echo "Deployment complete!"
