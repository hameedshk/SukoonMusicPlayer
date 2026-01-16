param(
    [switch]$FullBuild = $false
)

# ===== CONFIG =====
$APP_ID="com.sukoon.music"
$MAIN_ACTIVITY=".MainActivity"
$STATE_FILE=".last_successful_commit"
# ==================

# Ensure adb is available
adb get-state | Out-Null
$needsBuild = $false

if ($FullBuild) {
    Write-Host "FullBuild flag passed : forcing installDebug"
    $needsBuild = $true
}
elseif (!(Test-Path $STATE_FILE)) {
    Write-Host "First run : full build required"
    $needsBuild = $true
}
else {
    $LAST_COMMIT = Get-Content $STATE_FILE
    $changedFiles = git diff --name-only $LAST_COMMIT HEAD

    foreach ($file in $changedFiles) {
        if (
            $file -match "AndroidManifest.xml" -or
            $file -match "^app/src/main/res/" -or
			$file -match "^app/src/main/java/com/sukoon/music/" -or
            $file -match "build.gradle" -or
            $file -match "google-services.json"
        ) {
            $needsBuild = $true
            break
        }
    }
}

if ($needsBuild) {
    Write-Host "Running full build (installDebug)"
    ./gradlew installDebug --daemon
} else {
    Write-Host "Code-only changes : fast restart"
}

adb shell am force-stop $APP_ID
adb shell am start -n "$APP_ID/$MAIN_ACTIVITY"

# Save current commit hash
git rev-parse HEAD | Out-File $STATE_FILE -Force

Write-Host "App started running, continue your testing"

# Auto-detect (default)
#powershell -ExecutionPolicy Bypass -File smart_run.ps1

# Force full build
#powershell -ExecutionPolicy Bypass -File smart_run.ps1 -FullBuild:$true

