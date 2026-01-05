#!/usr/bin/env bash
set -euo pipefail

echo "== Pumpkin Android Codespaces: Installing Android SDK components =="

# Ensure sdkmanager is on PATH
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-/usr/local/android-sdk}
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"

# Accept licenses non-interactively
yes | sdkmanager --licenses >/dev/null

# Install minimal SDK parts needed to build (no emulator required)
sdkmanager \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0" \
  "cmdline-tools;latest"

echo "== Done. You can now build Android projects with Gradle =="
