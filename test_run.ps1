param(
    [switch]$FullBuild = $false,
    [switch]$VerboseBuild = $false,
    [switch]$Reinstall = $false,
    [switch]$ClearData = $false,
    [string[]]$LogTag,
    [switch]$Logcat = $false,
    [string]$PhoneIP = "192.168.0.152"
)

# ================= CONFIG =================
$APP_ID        = "com.sukoon.music"
$MAIN_ACTIVITY = ".MainActivity"
$STATE_HASH_FILE  = ".last_successful_state"
$STATE_FILES_FILE = ".last_successful_files"
$env:GRADLE_OPTS = "-Dorg.gradle.logging.level=info"
$BUILD_SUCCESS_FILE = ".last_build_success"

$ProgressPreference = 'SilentlyContinue'
$ErrorActionPreference = 'Continue'

$forceFullBuild = $FullBuild.IsPresent
$verboseGradle  = $VerboseBuild.IsPresent
$forceReinstall = $Reinstall.IsPresent
$forceClearData = $ClearData.IsPresent
$attachLogcat = $Logcat.IsPresent
$device = $env:ADB_DEVICE
$LAST_DEVICE_FILE = ".last_device"

# ============== DEVICE HELPERS ==============
# =========================================
# ANDROID LOCAL STABILITY AUTO TEST
# =========================================

# -------- CONFIGURATION (EDIT) ----------
$PACKAGE_NAME = "com.sukoon.music"
$APK_PATH = "app\build\outputs\apk\release\app-release-unsigned.apk"
$MONKEY_EVENTS = 15000
# =========================================
# ANDROID LOCAL STABILITY AUTO TEST
# Interactive Device + UI Test + Monkey + Crash Detection
# =========================================

# -------- CONFIGURATION (EDIT) ----------
$RUN_UI_TESTS = $true   # set $false if you want to skip UI tests
# ----------------------------------------

Write-Host ""
Write-Host "====================================="
Write-Host "ANDROID LOCAL STABILITY AUTO TEST"
Write-Host "====================================="
Write-Host ""

# =========================================
# DEVICE SELECTION (INTERACTIVE)
# =========================================

Write-Host "Detecting connected devices..." -ForegroundColor Cyan

$devicesRaw = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }

if (-not $devicesRaw) {
    Write-Host "No device/emulator connected ❌" -ForegroundColor Red
    exit 1
}

$devices = @()
$index = 1

foreach ($line in $devicesRaw) {
    $id = ($line -split "`t")[0]
    $devices += $id
    Write-Host "$index. $id"
    $index++
}

Write-Host ""
$choice = Read-Host "Enter device number to use"

if (-not ($choice -as [int]) -or $choice -lt 1 -or $choice -gt $devices.Count) {
    Write-Host "Invalid selection ❌" -ForegroundColor Red
    exit 1
}

$DEVICE_ID = $devices[$choice - 1]

Write-Host "Using device: $DEVICE_ID" -ForegroundColor Green
Write-Host ""

# =========================================
# STEP 1 — BUILD RELEASE APK
# =========================================

Write-Host "[1/6] Building Release APK..."
./gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED ❌" -ForegroundColor Red
    exit 1
}
Write-Host "Build Success ✅" -ForegroundColor Green

if (!(Test-Path $APK_PATH)) {
    Write-Host "APK NOT FOUND ❌ at $APK_PATH" -ForegroundColor Red
    exit 1
}

# =========================================
# STEP 2 — INSTALL APK
# =========================================

Write-Host ""
Write-Host "[2/6] Installing APK..."
adb -s $DEVICE_ID install -r $APK_PATH | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "INSTALL FAILED ❌" -ForegroundColor Red
    exit 1
}
Write-Host "Install Success ✅" -ForegroundColor Green

# =========================================
# STEP 3 — RUN UI TESTS (OPTIONAL)
# =========================================

if ($RUN_UI_TESTS) {
    Write-Host ""
    Write-Host "[3/6] Running UI Tests..."

    ./gradlew connectedAndroidTest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "UI TEST FAILED ❌ — Crash or test failure detected" -ForegroundColor Red
        exit 1
    }

    Write-Host "UI TEST PASSED ✅" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[3/6] Skipping UI Tests"
}

# =========================================
# STEP 4 — CLEAR LOGS
# =========================================

Write-Host ""
Write-Host "[4/6] Clearing old logs..."
adb -s $DEVICE_ID logcat -c

# =========================================
# STEP 5 — LAUNCH APP
# =========================================

Write-Host ""
Write-Host "[5/6] Launching App..."
adb -s $DEVICE_ID shell monkey -p $PACKAGE_NAME -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 5

# =========================================
# STEP 6 — MONKEY STRESS TEST
# =========================================

Write-Host ""
Write-Host "[6/6] Running Stress Test ($MONKEY_EVENTS events)..."

adb -s $DEVICE_ID shell monkey -p $PACKAGE_NAME `
    --throttle 120 `
    --ignore-crashes `
    --ignore-timeouts `
    --ignore-security-exceptions `
    -v $MONKEY_EVENTS | Out-File monkey_log.txt

Write-Host "Stress test completed"

# =========================================
# CRASH / ANR DETECTION
# =========================================

Write-Host ""
Write-Host "Checking for crashes..."

adb -s $DEVICE_ID logcat -d | Out-File crash_log.txt

$crashFound = Select-String -Path crash_log.txt -Pattern "FATAL EXCEPTION"
$anrFound = Select-String -Path crash_log.txt -Pattern "ANR in"

if ($crashFound) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host "CRASH DETECTED ❌ — FIX REQUIRED" -ForegroundColor Red
    Write-Host "See crash_log.txt for details"
    Write-Host "====================================="
    exit 1
}

if ($anrFound) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Yellow
    Write-Host "ANR DETECTED ⚠️ — PERFORMANCE ISSUE"
    Write-Host "====================================="
    exit 1
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "NO CRASH — BUILD STABLE ✅"
Write-Host "====================================="
Write-Host ""

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