param(
    [switch]$FullBuild = $false,
	[string]$PhoneIP = "192.168.0.152"
)

# ===== CONFIG =====
$APP_ID="com.sukoon.music"
$MAIN_ACTIVITY=".MainActivity"
$STATE_FILE=".last_successful_commit"
$PAIR_TIMEOUT   = 60   # seconds

# -------- DEVICE STATUS --------
function Prompt-For-AdbTarget {
    $ip = Read-Host "Enter device IP address (example: 192.168.0.140)"
    $port = Read-Host "Enter ADB port (example: 5555 or pairing port)"

    if (-not $ip -or -not $port) {
        Write-Error "IP and port are required"
        exit 1
    }

    return @{
        ip = $ip
        port = [int]$port
    }
}

function Try-Adb-Connect {
    param (
        [string]$Ip,
        [int]$Port
    )
    Write-Host "🔄 Trying adb connect to ${Ip}:${Port} ..." -ForegroundColor Cyan
    adb connect "${Ip}:${Port}" | Out-Null
    Start-Sleep -Seconds 2
}

function Get-Device-Status {
    $devices = adb devices

    foreach ($line in $devices) {
        if ($line -match "List of devices") { continue }
        if ([string]::IsNullOrWhiteSpace($line)) { continue }

        $parts = $line -split "\s+"
        if ($parts.Length -ge 2) {
            switch ($parts[1]) {
                "device"       { return "ONLINE" }
                "offline"      { return "OFFLINE" }
                "unauthorized" { return "UNAUTHORIZED" }
            }
        }
    }
    return "NONE"
}

# -------- MAIN FLOW --------

Write-Host "📱 Starting adb..." -ForegroundColor Cyan
adb start-server | Out-Null
Start-Sleep -Seconds 2

$status = Get-Device-Status

if ($status -eq "ONLINE") {
        Write-Host "✅ Device already connected via wireless TLS" -ForegroundColor Green
        # DO NOTHING — connection is valid
    }
else {
    Write-Host @"
⚠️ Device is OFFLINE.

Fix on phone:
• Unlock screen
• Toggle Wi-Fi once
• Toggle Wireless Debugging OFF → ON

Do NOT pair in this state.
"@
	$config = Prompt-For-AdbTarget 
    Try-Adb-Connect -Ip $config.ip -Port $config.port

    if ((Get-Device-Status) -eq "ONLINE") {
        Write-Host "✅ Connection successful — saving device info"
        #Save-AdbConfig -Ip $config.ip -Port $config.port
    }
    else {
        Write-Host "⚠️ adb connect did not succeed (TLS device or port closed)"
        Write-Host "ℹ️ Falling back to pairing / adb auto-discovery"        
    }
    }		

$needsBuild = $false

if ($FullBuild) {
    Write-Host "🔄 FullBuild flag passed : forcing installDebug" -ForegroundColor Cyan
    $needsBuild = $true
}
elseif (!(Test-Path $STATE_FILE)) {
    Write-Host "🆕 First run : full build required" -ForegroundColor Cyan
    $needsBuild = $true
}
else {
    # Check for committed changes
    $LAST_COMMIT = Get-Content $STATE_FILE
    $committedChanges = git diff --name-only $LAST_COMMIT HEAD

    # Check for uncommitted changes (staged + unstaged)
    $uncommittedChanges = git status --porcelain | ForEach-Object { $_.Substring(3) }

    # Combine both
    $allChanges = @($committedChanges) + @($uncommittedChanges) | Where-Object { $_ }

    if ($allChanges.Count -gt 0) {
        Write-Host "📝 Detected changes:" -ForegroundColor Yellow
        foreach ($file in $allChanges) {
            Write-Host "   • $file" -ForegroundColor Gray
        }
    }

    foreach ($file in $allChanges) {
        if (
            $file -match "AndroidManifest.xml" -or
            $file -match "^app/src/main/res/" -or
			$file -match "^app/src/main/java/" -or
            $file -match "build.gradle" -or
            $file -match "google-services.json"
        ) {
            Write-Host "⚡ Build-critical file changed: $file" -ForegroundColor Yellow
            $needsBuild = $true
            break
        }
    }
}

if ($needsBuild) {
    Write-Host "🔨 Running full build..." -ForegroundColor Cyan

    # Clean build cache
    Write-Host "🧹 Cleaning build cache..." -ForegroundColor Cyan
    ./gradlew clean

    # Assemble fresh APK
    Write-Host "📦 Building APK..." -ForegroundColor Cyan
    ./gradlew assembleDebug

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build failed" -ForegroundColor Red
        exit 1
    }

    # Uninstall old APK
    Write-Host "🗑️ Removing old APK..." -ForegroundColor Cyan
    adb uninstall -r $APP_ID | Out-Null
    Start-Sleep -Seconds 1

    # Install fresh APK
    Write-Host "📥 Installing fresh APK..." -ForegroundColor Cyan
    adb install "app/build/outputs/apk/debug/app-debug.apk"

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Installation failed" -ForegroundColor Red
        exit 1
    }

    Write-Host "✅ Installation successful" -ForegroundColor Green
} else {
    Write-Host "⚡ Code-only changes : fast restart" -ForegroundColor Cyan
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

#$pairing device
#adb pair 192.168.0.140:37573
#adb start-server
#adb kill-server
#adb connect 192.168.0.140:45679

