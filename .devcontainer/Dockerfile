FROM mcr.microsoft.com/devcontainers/base:ubuntu

ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies: JDK, Gradle, tools
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    gradle \
    unzip \
    wget \
    curl \
    git \
    zip \
    libstdc++6 \
    libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

# Android SDK paths
ENV ANDROID_SDK_ROOT=/usr/local/android-sdk
ENV ANDROID_HOME=/usr/local/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools:$ANDROID_SDK_ROOT/tools/bin

# Create SDK directory
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools

# Download Android command-line tools (Linux)
# (Google's official cmdline tools zip)
RUN cd /tmp \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip \
    && unzip -q cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm -f cmdline-tools.zip

# Permissions so the vscode user can write into SDK directory if needed
RUN chown -R vscode:vscode ${ANDROID_SDK_ROOT}
