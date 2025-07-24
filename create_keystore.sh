#!/bin/bash

# SMS Forward App Keystore Creation Script
# This script creates a keystore for signing the Android app

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "🔐 Creating SMS Forward App Keystore..."

# Set keystore file name
KEYSTORE_FILE="smsforward-release.keystore"
KEY_ALIAS="smsforward"

# Check if keystore already exists
if [ -f "$KEYSTORE_FILE" ]; then
    print_warning "Keystore file $KEYSTORE_FILE already exists!"
    read -p "Do you want to overwrite it? (y/n): " overwrite
    if [ "$overwrite" != "y" ] && [ "$overwrite" != "Y" ]; then
        print_status "Keystore creation cancelled."
        exit 0
    fi
    rm "$KEYSTORE_FILE"
fi

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    print_error "keytool command not found. Please ensure Java JDK is installed and in PATH."
    exit 1
fi

print_status "Creating keystore with the following details:"
echo "  Keystore file: $KEYSTORE_FILE"
echo "  Key alias: $KEY_ALIAS"
echo "  Key algorithm: RSA"
echo "  Key size: 2048 bits"
echo "  Validity: 25 years"
echo ""

print_warning "Please provide the following information for your keystore:"
echo "Note: Remember these details as you'll need them for app updates!"
echo ""

# Collect keystore information
read -s -p "Enter keystore password (min 6 characters): " KEYSTORE_PASSWORD
echo ""
read -s -p "Confirm keystore password: " KEYSTORE_PASSWORD_CONFIRM
echo ""

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    print_error "Passwords do not match!"
    exit 1
fi

if [ ${#KEYSTORE_PASSWORD} -lt 6 ]; then
    print_error "Password must be at least 6 characters long!"
    exit 1
fi

read -s -p "Enter key password (can be same as keystore password): " KEY_PASSWORD
echo ""

echo ""
print_status "Enter certificate information:"
read -p "First and Last Name (CN): " CN
read -p "Organizational Unit (OU): " OU
read -p "Organization (O): " O
read -p "City or Locality (L): " L
read -p "State or Province (ST): " ST
read -p "Country Code (C, 2 letters): " C

# Validate country code
if [ ${#C} -ne 2 ]; then
    print_error "Country code must be exactly 2 letters!"
    exit 1
fi

# Create the keystore
print_status "Creating keystore..."

keytool -genkey \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 9125 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

if [ $? -eq 0 ]; then
    print_success "Keystore created successfully: $KEYSTORE_FILE"
    
    # Create keystore properties file
    cat > keystore.properties << EOF
# Keystore configuration for SMS Forward App
# DO NOT commit this file to version control!
storeFile=$KEYSTORE_FILE
storePassword=$KEYSTORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF
    
    print_success "Keystore properties file created: keystore.properties"
    
    # Add to .gitignore if it exists
    if [ -f ".gitignore" ]; then
        if ! grep -q "keystore.properties" .gitignore; then
            echo "" >> .gitignore
            echo "# Keystore files - DO NOT COMMIT" >> .gitignore
            echo "*.keystore" >> .gitignore
            echo "keystore.properties" >> .gitignore
            print_success "Added keystore files to .gitignore"
        fi
    else
        cat > .gitignore << EOF
# Keystore files - DO NOT COMMIT
*.keystore
keystore.properties
EOF
        print_success "Created .gitignore with keystore exclusions"
    fi
    
    # Display keystore information
    print_status "Keystore information:"
    keytool -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD"
    
    echo ""
    print_success "🎉 Keystore setup completed!"
    print_warning "IMPORTANT SECURITY NOTES:"
    echo "  1. Keep your keystore file and passwords SECURE"
    echo "  2. Make a backup of your keystore file"
    echo "  3. NEVER commit keystore.properties to version control"
    echo "  4. You'll need the same keystore for all future app updates"
    echo ""
    
else
    print_error "Failed to create keystore!"
    exit 1
fi
