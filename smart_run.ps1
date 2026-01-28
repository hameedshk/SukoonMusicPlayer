param(
    [switch]$forceFullBuild = $false,
    [string]$PhoneIP = "192.168.0.152"
)

# ===== CONFIG =====
$APP_ID = "com.sukoon.music"
$MAIN_ACTIVITY = ".MainActivity"
$STATE_FILE = ".last_successful_state"
$PAIR_TIMEOUT = 60   # seconds
$env:GRADLE_OPTS = "-Dorg.gradle.logging.level=info"

# -------- DEVICE STATUS --------
function Prompt-For-AdbTarget {
    $ip = Read-Host "Enter device IP address (example: 192.168.0.140)"
    $port = Read-Host "Enter the port (example: 5555 or pairing port)"

    if (-not $ip -or -not $port) {
        Write-Error "IP and port are required"
        exit 1
    }

    return @{
        ip   = $ip
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
                "device" { return "ONLINE" }
                "offline" { return "OFFLINE" }
                "unauthorized" { return "UNAUTHORIZED" }
            }
        }
    }
    return "NONE"
}

function Get-WorkingTreeHash {
    git status --porcelain |
        Sort-Object |
        Out-String |
        ForEach-Object {
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($_)
            $sha = [System.Security.Cryptography.SHA1]::Create()
            ($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString("x2") }) -join ""
        }
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

$currentState = Get-WorkingTreeHash
$lastState = if (Test-Path $STATE_FILE) { Get-Content $STATE_FILE } else { "" }

# ---------- SKIP LOGIC (DISABLED FOR FULL BUILD) ----------
if (-not $forceFullBuild -and $currentState -eq $lastState) {
    Write-Host "⚡ No changes since last successful run — skipping build" -ForegroundColor Green
    adb shell "am force-stop $APP_ID; am start -n $APP_ID/$MAIN_ACTIVITY"
    exit 0
}

# ---------- CHANGE ANALYSIS ----------
$needsBuild = $false
$needsClean = $false

$changedFiles = git status --porcelain | ForEach-Object { $_.Substring(3) }

foreach ($file in $changedFiles) {

    if ($file -match "build.gradle$" -or
        $file -match "build.gradle.kts$" -or
        $file -match "settings.gradle" -or
        $file -match "gradle.properties") {

        $needsClean = $true
        $needsBuild = $true
        break
    }

    if ($file -match "AndroidManifest.xml" -or
        $file -match "^app/src/main/res/" -or
        $file -match "^app/src/main/java/" -or
        $file -match "^app/src/main/kotlin/" -or
        $file -match "google-services.json") {

        $needsBuild = $true
    }
}


# ---------- FORCE FULL BUILD ----------
if ($forceFullBuild) {
    Write-Host "🔄 forceFullBuild requested — ignoring cached state" -ForegroundColor Cyan
    $needsClean = $true
    $needsBuild = $true
}
# ---------- BUILD ----------
if ($needsClean) {
    Write-Host "🧹 CLEAN build" -ForegroundColor Cyan
    ./gradlew clean :app:installDebug --console=plain --profile |
        ForEach-Object {
            if ($_ -match "took\s+([\d\.]+)\s+secs") {
                Write-Host $_ -ForegroundColor Yellow
            }
        }
}
elseif ($needsBuild) {
    Write-Host "🚀 INCREMENTAL build" -ForegroundColor Cyan
    ./gradlew :app:installDebug --console=plain --profile |
        ForEach-Object {
            if ($_ -match "took\s+([\d\.]+)\s+secs") {
                Write-Host $_ -ForegroundColor Yellow
            }
        }
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed" -ForegroundColor Red
    exit 1
}

# ---------- SAVE STATE ----------
Get-WorkingTreeHash | Out-File $STATE_FILE -Force
Write-Host "App started running, continue your testing"

# Auto-detect (default)
#powershell -ExecutionPolicy Bypass -File smart_run.ps1

# Force full build
#powershell -ExecutionPolicy Bypass -File smart_run.ps1 -forceFullBuild true

#$pairing device
#adb pair 192.168.0.140:37573
#adb start-server
#adb kill-server
#adb connect 192.168.0.140:45679
#logs check 
#adb logcat --pid=$(adb shell pidof -s com.sukoon.music) | Select-String "ContinueListeningCard"
#todos
#ensure typed ip device is conncected
#work on pairing 
