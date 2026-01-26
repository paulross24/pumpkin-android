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
- Sends POST /ingest (v1 schema) to the configured Pumpkin server
- Optional foreground location inclusion (last known location only)

## Ingest v1 contract
The app sends:
- `schema_version: 1`
- `request_id: <uuid>`
- `text`, `source`, `device`, `ts`
- optional `location`

If the server has `PUMPKIN_INGEST_KEY` set, the app sends `X-Pumpkin-Key`.

## Screens
- Home: current server + key summary
- Push: text input and response log
- Settings: server URL, API key, include location toggle
- Debug: last response/error
