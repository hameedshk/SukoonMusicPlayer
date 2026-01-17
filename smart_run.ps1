param(
    [switch]$FullBuild = $true,
	[string]$PhoneIP = "192.168.0.140"
)

# ===== CONFIG =====
$APP_ID="com.sukoon.music"
$MAIN_ACTIVITY=".MainActivity"
$STATE_FILE=".last_successful_commit"

$ADB_PORT=5555
$PAIR_TIMEOUT   = 60   # seconds


function Is-Device-Connected {
    $devices = adb devices
    return ($devices | Select-String "_adb")
}


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
    Write-Host "------------------------------------------------"
    Write-Host "1. Open Settings → Developer Options"
    Write-Host "2. Open Wireless Debugging"
    Write-Host "3. Tap 'Pair device with pairing code'"
    Write-Host "4. Keep this screen OPEN"
    Write-Host "------------------------------------------------"
    Write-Host ""

    $elapsed = 0
    while ($elapsed -lt $PAIR_TIMEOUT) {
        $pairingPort = Get-PairingPort
        if ($pairingPort) {
            return $pairingPort
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }

    Write-Error "❌ Pairing service not detected within $PAIR_TIMEOUT seconds."
    exit 1
}


function Pair-Device {
    $pairingPort = Wait-For-PairingService

    Write-Host "🔐 Pairing service detected on port $pairingPort" -ForegroundColor Cyan
    $pairCode = Read-Host "Enter the 6-digit pairing code shown on phone"

    adb pair "${PhoneIP}:${pairingPort}" $pairCode

    if ($LASTEXITCODE -ne 0) {
        Write-Error "❌ Pairing failed. Code may be wrong or expired."
        exit 1
    }

    Write-Host "✅ Pairing successful (this is required only once)" -ForegroundColor Green
}


function Connect-Device {
    adb connect "${PhoneIP}:${ADB_PORT}" | Out-Null
    Start-Sleep -Seconds 2

    if (-not (Is-Device-Connected)) {
        Write-Error "❌ Device not connected after pairing"
        exit 1
    }

    Write-Host "🚀 Device connected via wireless ADB" -ForegroundColor Green
}


# ---------------- MAIN FLOW ----------------

Write-Host ""
Write-Host "📱 Checking ADB connection..." -ForegroundColor Cyan

if (Is-Device-Connected) {
    Write-Host "✅ Device already paired & connected (auto-reuse keys)" -ForegroundColor Green
}
else {
    Write-Host "⚠️ Device not connected. Pairing required." -ForegroundColor Yellow
    Pair-Device
    Connect-Device
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

