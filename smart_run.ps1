# ===== CONFIG =====
$APP_ID="com.sukoon.music"
$MAIN_ACTIVITY=".MainActivity"
$STATE_FILE=".last_successful_commit"
# ==================

# Ensure adb is available
adb get-state | Out-Null

# First run: no state file
if (!(Test-Path $STATE_FILE)) {
    Write-Host "First run → full build required"
    $needsBuild = $true
} else {
    $LAST_COMMIT = Get-Content $STATE_FILE
    $changedFiles = git diff --name-only $LAST_COMMIT HEAD
    $needsBuild = $false

    foreach ($file in $changedFiles) {
        if (
            $file -match "AndroidManifest.xml" -or
            $file -match "^app/src/main/res/" -or
            $file -match "build.gradle" -or
            $file -match "google-services.json"
        ) {
            $needsBuild = $true
            break
        }
    }
}

if ($needsBuild) {
    Write-Host " Structural changes detected → installDebug"
    ./gradlew installDebug --daemon
} else {
    Write-Host "Code-only changes → fast restart"
}

adb shell am force-stop $APP_ID
adb shell am start -n "$APP_ID/$MAIN_ACTIVITY"

# Save current commit hash
git rev-parse HEAD | Out-File $STATE_FILE -Force

Write-Host "App running"
