# Pumpkin Android (Codespaces)

This repo is set up to develop an Android app for Pumpkin using GitHub Codespaces (no admin rights required).

## What works in Codespaces
- Edit Kotlin + Jetpack Compose code
- Run Gradle builds (assembleDebug)
- Produce APK artifacts

## What does NOT work in Codespaces
- Android emulator (use Android Studio at home for emulator/device testing)

## Quick start
1. Open this repo in Codespaces
2. Build:
   ./gradlew assembleDebug

## App overview
- Single-activity Jetpack Compose app with MVVM
- Sends POST /ingest to the configured Pumpkin server
- Optional foreground location inclusion (last known location only)

## Screens
- Home: current server + key summary
- Push: text input and response log
- Settings: server URL, API key, include location toggle
- Debug: last response/error
