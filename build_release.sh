#!/bin/bash

# SMS Forward App Release Build Script
# Author: hentiflo

echo "🚀 Starting SMS Forward App Release Build..."

# Set up Java environment
ANDROID_STUDIO_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -d "$ANDROID_STUDIO_JDK" ]; then
    export JAVA_HOME="$ANDROID_STUDIO_JDK"
    export PATH="$JAVA_HOME/bin:$PATH"
    print_status "Java environment set up using Android Studio JDK"
else
    print_error "Android Studio JDK not found. Please run ./setup_java_env.sh first"
    exit 1
fi

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

# Check if we're in the right directory
if [ ! -f "app/build.gradle" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Step 1: Clean the project
print_status "Cleaning project..."
./gradlew clean
if [ $? -eq 0 ]; then
    print_success "Project cleaned successfully"
else
    print_error "Failed to clean project"
    exit 1
fi

# Step 2: Run tests (optional)
read -p "Do you want to run tests before building? (y/n): " run_tests
if [ "$run_tests" = "y" ] || [ "$run_tests" = "Y" ]; then
    print_status "Running tests..."
    ./gradlew test
    if [ $? -eq 0 ]; then
        print_success "All tests passed"
    else
        print_warning "Some tests failed, but continuing with build..."
    fi
fi

# Step 3: Build release APK
print_status "Building release APK..."
./gradlew assembleRelease
if [ $? -eq 0 ]; then
    print_success "Release APK built successfully"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_success "APK location: $APK_PATH"
        print_success "APK size: $APK_SIZE"
    fi
else
    print_error "Failed to build release APK"
    exit 1
fi

# Step 4: Build release AAB (Android App Bundle)
print_status "Building release AAB..."
./gradlew bundleRelease
if [ $? -eq 0 ]; then
    print_success "Release AAB built successfully"
    AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
    if [ -f "$AAB_PATH" ]; then
        AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
        print_success "AAB location: $AAB_PATH"
        print_success "AAB size: $AAB_SIZE"
    fi
else
    print_error "Failed to build release AAB"
    exit 1
fi

# Step 5: Generate checksums
print_status "Generating checksums..."
if [ -f "$APK_PATH" ]; then
    APK_SHA256=$(shasum -a 256 "$APK_PATH" | cut -d' ' -f1)
    echo "$APK_SHA256  app-release.apk" > app-release.apk.sha256
    print_success "APK SHA256: $APK_SHA256"
fi

if [ -f "$AAB_PATH" ]; then
    AAB_SHA256=$(shasum -a 256 "$AAB_PATH" | cut -d' ' -f1)
    echo "$AAB_SHA256  app-release.aab" > app-release.aab.sha256
    print_success "AAB SHA256: $AAB_SHA256"
fi

# Step 6: Create release directory
RELEASE_DIR="release_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RELEASE_DIR"

# Copy files to release directory
if [ -f "$APK_PATH" ]; then
    cp "$APK_PATH" "$RELEASE_DIR/"
    cp "app-release.apk.sha256" "$RELEASE_DIR/"
fi

if [ -f "$AAB_PATH" ]; then
    cp "$AAB_PATH" "$RELEASE_DIR/"
    cp "app-release.aab.sha256" "$RELEASE_DIR/"
fi

# Copy release notes template
cat > "$RELEASE_DIR/RELEASE_NOTES.md" << EOF
# SMS Forward App Release Notes

## Version: $(grep versionName app/build.gradle | sed 's/.*"\(.*\)".*/\1/')
## Build Date: $(date)

### New Features
- [ ] Feature 1
- [ ] Feature 2

### Bug Fixes
- [ ] Fix 1
- [ ] Fix 2

### Improvements
- [ ] Improvement 1
- [ ] Improvement 2

### Known Issues
- [ ] Issue 1

### Installation Instructions
1. Download the APK file
2. Enable "Install from unknown sources" in Android settings
3. Install the APK
4. Grant necessary permissions

### Checksums
- APK SHA256: $APK_SHA256
- AAB SHA256: $AAB_SHA256
EOF

print_success "Release files created in: $RELEASE_DIR"

# Step 7: Optional - Install on connected device
read -p "Do you want to install the APK on a connected device? (y/n): " install_apk
if [ "$install_apk" = "y" ] || [ "$install_apk" = "Y" ]; then
    print_status "Installing APK on connected device..."
    adb install -r "$APK_PATH"
    if [ $? -eq 0 ]; then
        print_success "APK installed successfully"
    else
        print_warning "Failed to install APK (make sure device is connected and USB debugging is enabled)"
    fi
fi

echo ""
print_success "🎉 Release build completed successfully!"
print_status "Next steps:"
echo "  1. Test the release build thoroughly"
echo "  2. Update RELEASE_NOTES.md with actual changes"
echo "  3. Upload to app stores or distribute as needed"
echo "  4. Tag the release in git: git tag v$(grep versionName app/build.gradle | sed 's/.*"\(.*\)".*/\1/')"
echo ""
