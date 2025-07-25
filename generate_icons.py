#!/usr/bin/env python3
"""
Android Icon Generator for SMSForward App
Generates all required icon sizes from SVG source
"""

import os
import subprocess
import sys
from pathlib import Path

# Icon sizes for different densities
ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

# Paths
SVG_SOURCE = "app_icon_simple.svg"
OUTPUT_BASE = "app/src/main/res"

def check_dependencies():
    """Check if required tools are available"""
    try:
        subprocess.run(['inkscape', '--version'], capture_output=True, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        print("Error: Inkscape is required but not found.")
        print("Please install Inkscape:")
        print("  macOS: brew install inkscape")
        print("  Ubuntu: sudo apt install inkscape")
        print("  Windows: Download from https://inkscape.org/")
        return False

def generate_png_from_svg(svg_path, output_path, size):
    """Generate PNG from SVG using Inkscape"""
    try:
        cmd = [
            'inkscape',
            '--export-type=png',
            f'--export-filename={output_path}',
            f'--export-width={size}',
            f'--export-height={size}',
            svg_path
        ]
        subprocess.run(cmd, check=True, capture_output=True)
        print(f"✓ Generated {output_path} ({size}x{size})")
        return True
    except subprocess.CalledProcessError as e:
        print(f"✗ Failed to generate {output_path}: {e}")
        return False

def create_webp_from_png(png_path, webp_path):
    """Convert PNG to WebP format"""
    try:
        # Try using cwebp if available
        cmd = ['cwebp', '-q', '90', png_path, '-o', webp_path]
        subprocess.run(cmd, check=True, capture_output=True)
        print(f"✓ Generated WebP: {webp_path}")
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        # Fallback: just copy PNG as WebP (Android supports PNG in .webp files)
        import shutil
        shutil.copy2(png_path, webp_path)
        print(f"✓ Copied as WebP: {webp_path}")
        return True

def generate_all_icons():
    """Generate all required icon sizes"""
    if not check_dependencies():
        return False
    
    if not os.path.exists(SVG_SOURCE):
        print(f"Error: SVG source file '{SVG_SOURCE}' not found")
        return False
    
    success_count = 0
    total_count = 0
    
    for density, size in ICON_SIZES.items():
        # Create output directory
        output_dir = Path(OUTPUT_BASE) / f"mipmap-{density}"
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate regular icon
        png_path = output_dir / "ic_launcher.png"
        webp_path = output_dir / "ic_launcher.webp"
        
        total_count += 1
        if generate_png_from_svg(SVG_SOURCE, str(png_path), size):
            success_count += 1
            create_webp_from_png(str(png_path), str(webp_path))
            # Remove PNG after creating WebP
            os.remove(str(png_path))
        
        # Generate round icon (same as regular for this design)
        png_round_path = output_dir / "ic_launcher_round.png"
        webp_round_path = output_dir / "ic_launcher_round.webp"
        
        total_count += 1
        if generate_png_from_svg(SVG_SOURCE, str(png_round_path), size):
            success_count += 1
            create_webp_from_png(str(png_round_path), str(webp_round_path))
            # Remove PNG after creating WebP
            os.remove(str(png_round_path))
    
    print(f"\nGeneration complete: {success_count}/{total_count} icons generated successfully")
    return success_count == total_count

def create_adaptive_icon_xml():
    """Create adaptive icon XML files"""
    anydpi_dir = Path(OUTPUT_BASE) / "mipmap-anydpi-v26"
    anydpi_dir.mkdir(parents=True, exist_ok=True)
    
    # ic_launcher.xml
    launcher_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>'''
    
    with open(anydpi_dir / "ic_launcher.xml", "w") as f:
        f.write(launcher_xml)
    
    # ic_launcher_round.xml
    with open(anydpi_dir / "ic_launcher_round.xml", "w") as f:
        f.write(launcher_xml)
    
    print("✓ Created adaptive icon XML files")

if __name__ == "__main__":
    print("SMSForward Icon Generator")
    print("=" * 30)
    
    if generate_all_icons():
        create_adaptive_icon_xml()
        print("\n🎉 All icons generated successfully!")
        print("\nNext steps:")
        print("1. Build and test your app")
        print("2. Check icons in Android Studio's Resource Manager")
        print("3. Test on different devices and Android versions")
    else:
        print("\n❌ Some icons failed to generate")
        sys.exit(1)
