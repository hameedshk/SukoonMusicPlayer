param(
    [switch]$FullBuild = $false,
	[string]$PhoneIP = "192.168.0.140"
)

# ===== CONFIG =====
$APP_ID="com.sukoon.music"
$MAIN_ACTIVITY=".MainActivity"
$STATE_FILE=".last_successful_commit"
$PAIR_TIMEOUT   = 60   # seconds

# -------- DEVICE STATUS --------
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


# -------- PAIRING DISCOVERY --------
function Get-PairingPort {
    $services = adb mdns services
    foreach ($line in $services) {
        if ($line -match "_adb-pairing\._tcp\." -and $line -match ":(\d+)") {
            return $matches[1]
        }
    }
    return $null
}


function Wait-For-PairingService {
    Write-Host ""
    Write-Host "🔐 ACTION REQUIRED ON PHONE" -ForegroundColor Yellow
    Write-Host "--------------------------------------------"
    Write-Host "1. Open Settings → Developer Options"
    Write-Host "2. Open Wireless Debugging"
    Write-Host "3. Tap 'Pair device with pairing code'"
    Write-Host "4. KEEP THIS SCREEN OPEN"
    Write-Host "--------------------------------------------"
    Write-Host ""

    $elapsed = 0
    while ($elapsed -lt $PAIR_TIMEOUT) {
        adb start-server | Out-Null
        $pairingPort = Get-PairingPort
        if ($pairingPort) { return $pairingPort }

        Start-Sleep -Seconds 3
        $elapsed += 3
    }

    Write-Error "❌ Pairing service not detected. Toggle Wireless Debugging and retry."
    exit 1
}


function Pair-Device {
    $pairingPort = Wait-For-PairingService
    $pairCode = Read-Host "Enter 6-digit pairing code"

    adb pair "localhost:$pairingPort" $pairCode

    if ($LASTEXITCODE -ne 0) {
        Write-Error "❌ Pairing failed. Code expired or incorrect."
        exit 1
    }

    Write-Host "✅ Pairing successful (one-time)" -ForegroundColor Green
}


# -------- MAIN FLOW --------

Write-Host "📱 Starting adb..." -ForegroundColor Cyan
adb start-server | Out-Null
Start-Sleep -Seconds 2

$status = Get-Device-Status

switch ($status) {

    "ONLINE" {
        Write-Host "✅ Device already connected via wireless TLS" -ForegroundColor Green
        # DO NOTHING — connection is valid
    }

    "OFFLINE" {
        Write-Error @"
⚠️ Device is OFFLINE.

Fix on phone:
• Unlock screen
• Toggle Wi-Fi once
• Toggle Wireless Debugging OFF → ON

Do NOT pair in this state.
"@
        exit 1
    }

    "UNAUTHORIZED" {
        Write-Error "🔐 Accept debugging authorization on phone"
        exit 1
    }

    "NONE" {
        Write-Host "❌ No device detected"
        Write-Host "🔐 Pairing required" -ForegroundColor Yellow
        Pair-Device
    }
}

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
			$file -match "^app/src/main/java/" -or
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

#$pairing device
#adb pair 192.168.0.140:37573
#adb start-server
#adb kill-server
#adb connect 192.168.0.140:45679

